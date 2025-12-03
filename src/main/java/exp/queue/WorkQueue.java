package exp.queue;

import exp.entity.Node;
import exp.entity.Value;
import exp.entity.Work;
import exp.svc.NodeGroupService;
import exp.svc.NodeService;
import exp.svc.ValueService;
import exp.svc.WorkService;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class WorkQueue implements BlockingQueue<Runnable> {
    class WorkRunner implements Runnable {
        private Work work;
        private Counters<Node> counters;

        public WorkRunner(Work work) {
            this.work = work;
        }

        public Work getWork() {return work;}

        @Override
        public void run() {
            //have to use a parent class method because @Transactional on run is invalid
            //"[...] declares an interceptor binding, but it must be ignored per CDI rules"
            doWork(work);
        }
    }
    List<Work> todo;
    private Counters<Node> counters = new Counters<>();

    private final AtomicInteger count = new AtomicInteger();
    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();
    private final ReentrantLock putLock = new ReentrantLock();


    private final WorkService workService;
    private final NodeGroupService nodeGroupService;
    private final NodeService nodeService;
    private final ValueService valueService;

    public WorkQueue(WorkService workService, NodeGroupService nodeGroupService, NodeService nodeService, ValueService valueService) {
        this.workService = workService;
        this.nodeGroupService = nodeGroupService;
        this.nodeService = nodeService;
        this.valueService = valueService;

        counters.setCallback("onComplete",this::onComplete);

        //replace with a better performant option
        this.todo = new CopyOnWriteArrayList<>();
    }

    public void addCallback(String name, Consumer<Node> callback){
        counters.setCallback(name,callback);
    }
    public boolean hasCallback(String name){
        return counters.hasCallback(name);
    }
    public void removeCallback(String name){
        counters.removeCallback(name);
    }
    public int counterSum(){
        return counters.sum();
    }

    public boolean hasWorkService(){return workService != null;}
    public boolean hasNodeGroupService(){return nodeGroupService != null;}
    public boolean hasNodeService(){return nodeService != null;}
    public boolean hasValueService(){return valueService != null;}

    public void onComplete(Node n){
        takeLock.lock();
        try {
            if(!todo.isEmpty()){
                //signal all because this could unblock multiple work items
                notEmpty.signalAll();
            }
        } finally {
            takeLock.unlock();
        }
    }

    @Transactional
    public void doWork(Work work){
        try {
            if(work.activeNode==null || work.sourceValues == null || work.sourceValues.isEmpty()){
                //error conditions
            }
            //looping over values works for Jq / Js nodes but what about cross test comparison
            //calculateValue should probably accept all sourceValues and leave it to the node function to decide
            List<Value> calculated = nodeService.calculateValues(work.activeNode,work.sourceValues);
            //TODO purge existing values
            for(Value v : work.sourceValues) {
                valueService.deleteDescendantValues(v, work.activeNode);
            }
            //TODO change drop drop and replace to update data?
            //does this break descendant values or do we assume they will recalculate?
            for(Value newValue : calculated){
                valueService.create(newValue);
            }
            //not in the finally so that it only happens if the work succeeds
            //TODO this is throwing TransactionRequiredException
            workService.delete(work);
        }catch( Exception e){
            //TODO how to handle the exception, adding it back to the todo list
            addWork(work);
        } finally {
            if(work.activeNode!=null){
                counters.decrement(work.activeNode);
            }
        }
    }

    private void fullyLock(){
        takeLock.lock();
        putLock.lock();
    }
    private void fullyUnlock(){
        takeLock.unlock();
        putLock.unlock();
    }
    private void signalNotEmpty(){
        takeLock.lock();
        try{
            notEmpty.signal();
        }finally {
            takeLock.unlock();
        }
    }

    private Work removeFirstUnblocked(){

        Work found = null;
        int idx = 0;
        int score = -1;

        fullyLock();
        try {
            if(todo.isEmpty()){
                return null;
            }
            do {
                found = todo.get(idx);
                score = found.sourceNodes.stream().mapToInt(n -> counters.get(n)).sum();
                if (score > 0) {
                    found = null;
                }
                idx++;
            } while (idx < todo.size() && score > 0);
            if (found != null) {
                Work removed = todo.remove(idx - 1); //index of found
                assert !todo.contains(found);
            }
        }finally {
            fullyUnlock();
        }
        return found;
    }
    public void addWorks(Collection<Work> works){
        putLock.lock();
        try {
            works.forEach(todo::add);
            int c = count.getAndAdd(works.size());
            works.forEach(work -> {
                if (work.activeNode != null) {
                    counters.increment(work.activeNode);
                }
            });
            todo = KahnDagSort.sort(todo, w -> w.getDependentWorks(getTodo()));
            if(c == 0){
                signalNotEmpty();
            }
        }finally {
            putLock.unlock();
        }
    }
    public void addWork(Work work) {
        putLock.lock();
        try {
            todo.add(work);
            int c = count.getAndIncrement();
            if (work.activeNode != null) {
                counters.increment(work.activeNode);
            }
            todo = KahnDagSort.sort(todo, w -> w.getDependentWorks(getTodo()));
            if(c == 0){
                signalNotEmpty();
            }
        }finally {
            putLock.unlock();
        }
    }
    public boolean hasWork(Work work){
        return todo.contains(work);
    }

    public List<Work> getTodo(){return new ArrayList<>(todo);}


    @Override
    public boolean add(Runnable runnable) {
        //This is not supported
        return false;
    }

    @Override
    public boolean offer(Runnable runnable) {
        //This is not supported
        return false;
    }

    @Override
    public Runnable remove() {
        Runnable r = poll();
        if(r == null){
            throw new NoSuchElementException();
        }
        return r;
    }

    @Override
    public Runnable poll() {
        if(count.get()==0){
            return null;
        }
        takeLock.lock();
        try{
            if(count.get()==0){
                return null;
            }
            Work found = removeFirstUnblocked();
            if(found == null){
                return null;
            }
            int c = count.getAndDecrement();
            if(c > 1){
                notEmpty.signal();
            }
            return new WorkRunner(found);
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public Runnable element() {
        Runnable rtrn = peek();
        if(rtrn==null){
            throw new NoSuchElementException();
        }
        return rtrn;
    }

    @Override
    public Runnable peek() {
        if(count.get()==0){
            return null;
        }
        takeLock.lock();
        try{
            return count.get() > 0 ? new WorkRunner(todo.get(0)) : null;
        }finally {
            takeLock.unlock();
        }
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        //not supported
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        //not supported
        return false;
    }

    @Override
    public Runnable take() throws InterruptedException {
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            Work work = null;
            do {
                work = removeFirstUnblocked();
                if (work == null) {
                    notEmpty.await();
                }
            } while (work == null);
            int c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal();
            }
            return new WorkRunner(work);
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        takeLock.lockInterruptibly();
        try {
            while(count.get()==0){
                if (nanos <= 0L){
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            Work work = null;
            while(work == null){
                work = removeFirstUnblocked();
                if(work == null){
                    if (nanos <= 0L){
                        return null;
                    }
                    nanos = notEmpty.awaitNanos(nanos);
                }
            }
            int c = count.getAndDecrement();
            if( c > 1){
                notEmpty.signal();
            }
            return new WorkRunner(work);
        } finally {
            takeLock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE - count.get();
    }

    @Override
    public boolean remove(Object o) {
        boolean rtrn = false;
        if(o instanceof Work){
            Work work = (Work)o;
            if(todo.contains(work)){
                fullyLock();
                try{
                    rtrn = todo.remove(work);
                } finally {
                    fullyUnlock();
                }
            }
        }
        return rtrn;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return todo.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Runnable> c) {
        //not supported
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        todo.removeAll(c);
        //removing should not require a resport as the items are already sorted
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        todo.retainAll(c);
        todo = KahnDagSort.sort(todo,w->w.getDependentWorks(getTodo()));
        return false;
    }

    @Override
    public void clear() {
        fullyLock();
        try {
            todo.clear();
        }finally {
            fullyUnlock();
        }
    }

    @Override
    public int size() {
        return todo.size();
    }

    @Override
    public boolean isEmpty() {
        return todo.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return todo.contains(o);
    }

    @Override
    public Iterator<Runnable> iterator() {
        //not supported
        return null;
    }

    @Override
    public Object[] toArray() {
        return todo.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return todo.toArray(a);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c) {
        //not supported
        return 0;
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        //not supported
        return 0;
    }
}




