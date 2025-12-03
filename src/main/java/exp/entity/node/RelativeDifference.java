package exp.entity.node;

import exp.entity.Node;
import exp.entity.Value;
import io.hyperfoil.tools.yaup.json.Json;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Entity
@DiscriminatorValue("rd")
public class RelativeDifference extends Node {

    private static final String THRESHOLD = "threshold";
    private static final String WINDOW = "window";
    private static final String MIN_PREVIOUS = "minPrevious";
    private static final String FILTER =  "filter";

    @Transient
    private Json config;

    public RelativeDifference() {}
    public RelativeDifference(String name, String operation) {
        super(name,operation);
    }

    @PostLoad
    public void loadConfig(){
        if(this.config == null){
            if(this.operation!=null && !this.operation.isBlank()){
                config = Json.fromString(this.operation);
            }else {
                config = new Json();
                //TODO load default values?
            }
        }
    }
    @Transient
    public Node getRangeNode(){
        return sources.get(0);
    }
    @Transient
    public Node getDomainNode(){
        return sources.get(1);
    }

    @Transient
    public List<Node> getFingerprintNodes(){
        return sources.subList(2,sources.size());
    }

    @Transient
    public double getThreshold(){
        return config.getDouble(THRESHOLD,.2);
    }
    public void setThreshold(double threshold){
        config.set(THRESHOLD,threshold);
        operation=config.toString();
    }
    @Transient
    public long getWindow(){
        return config.getLong(WINDOW,1);
    }
    public void setWindow(long window){
        config.set(WINDOW,window);
        operation=config.toString();
    }
    @Transient
    public long getMinPrevious(){
        return config.getLong(MIN_PREVIOUS,5);
    }
    public void setMinPrevious(long minPrevious){
        config.set(MIN_PREVIOUS,minPrevious);
        operation=config.toString();
    }
    @Transient
    public String getFilter(){
        return config.getString(FILTER);
    }
    public void setFilter(String filter){
        config.set(FILTER,filter);
        operation=config.toString();
    }

    @Override
    protected Node shallowCopy() {
        return new RelativeDifference(name,operation);
    }

}
