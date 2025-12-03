package exp.command;

import exp.svc.FolderService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name="folder", aliases={"folders"}, description = "list folders", mixinStandardHelpOptions = true)
public class ListFolder implements Runnable {

    @CommandLine.ParentCommand
    ListCmd listCmd;

    @Inject
    FolderService folderService;

    @Override
    public void run() {
        System.out.println(ListCmd.table(80,folderService.list(),List.of("name"), List.of(f->f.name)));
        //folderService.list().forEach(System.out::println);
    }
}
