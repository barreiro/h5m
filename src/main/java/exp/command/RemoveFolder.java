package exp.command;

import exp.entity.Folder;
import exp.svc.FolderService;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name="folder",description = "remove a folder", mixinStandardHelpOptions = true)
public class RemoveFolder implements Callable<Integer> {

    @Inject
    FolderService folderService;

    @CommandLine.Parameters
    String name;

    @Override
    public Integer call() throws Exception {
        Folder found = folderService.byName(name);
        if(found == null){
            System.out.println("Folder "+name+" not found");
        }else{
            folderService.delete(found);
        }
        return 0;
    }
}
