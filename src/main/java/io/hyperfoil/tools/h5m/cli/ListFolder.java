package io.hyperfoil.tools.h5m.cli;

import io.hyperfoil.tools.h5m.api.Folder;
import io.hyperfoil.tools.h5m.api.FolderStatus;
import io.hyperfoil.tools.h5m.api.svc.FolderServiceInterface;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name="folder", aliases={"folders"}, description = "list folders", mixinStandardHelpOptions = true)
public class ListFolder implements Runnable {

    @CommandLine.ParentCommand
    ListCmd listCmd;

    @Inject
    FolderServiceInterface folderService;

    @Override
    public void run() {
        List<Long> ids = folderService.list().stream().map(Folder::id).toList();
        List<FolderStatus> summaries = folderService.getFolderStatus(ids).stream()
                .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name())).toList();
        System.out.println(ListCmd.table(80, summaries, List.of("name", "uploads"),
                List.of(FolderStatus::name, FolderStatus::uploadCount)));
    }
}
