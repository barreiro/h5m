package exp.command;

import exp.entity.Folder;
import exp.entity.NodeGroup;
import exp.svc.FolderService;
import exp.svc.NodeGroupService;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name="folder", description = "add a folder", mixinStandardHelpOptions = true)
public class AddFolder implements Callable<Integer> {

    @Inject
    FolderService folderService;

    @Inject
    NodeGroupService nodeGroupService;

    @CommandLine.Parameters(index="0",arity="0..1")
    public String name;
    @CommandLine.Parameters(index="1",arity="0..1")
    public String folder;


    @Override
    public Integer call() throws Exception {
        if(name == null && H5m.consoleAttached()){
            Scanner sc = new Scanner(System.in);
            System.out.printf("Enter name: ");
            name = sc.nextLine();
        }
        if(folder == null && H5m.consoleAttached()){
            Scanner sc = new Scanner(System.in);
            System.out.printf("Enter path: ");
            folder = sc.nextLine();
        }
        if(".".equals(folder) || "./".equals(folder)){
            folder = Paths.get(".").toAbsolutePath().normalize().toString();
        }
        Folder existing = folderService.byName(folder);
        if(existing != null){
            System.err.println(name+" already exists");
            return 1;
        }
        existing = folderService.byPath(folder);
        if(existing != null){
            System.err.println(existing.name+" already exists for "+folder);
            return 1;
        }
        NodeGroup existingGroup =  nodeGroupService.byName(folder);
        if(existingGroup != null){
            System.err.println(name+" conflicts with an existing node group");
            return 1;
        }
        Folder newFolder = new Folder(name,folder);
        folderService.create(newFolder);
        return 0;
    }
}
