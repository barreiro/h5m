package io.hyperfoil.tools.h5m.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Summary statistics for a folder")
public record FolderStatus(
        @Schema(description = "Unique folder ID") Long id,
        @Schema(description = "Folder name") String name,
        @Schema(description = "Number of uploads") int uploadCount,
        @Schema(description = "Number of nodes in the pipeline") int nodeCount,
        @Schema(description = "Number of detected changes") int changeCount,
        @Schema(description = "Timestamp of the last upload") LocalDateTime lastUpload,
        @Schema(description = "Timestamp of the last detected change") LocalDateTime lastChange) {
}
