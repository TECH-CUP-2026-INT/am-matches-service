package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import co.edu.escuelaing.techcup.match.entity.MatchSheet;

public final class MatchSheetMapper {

    private MatchSheetMapper() {
    }

    public static MatchSheetResponse toResponse(MatchSheet sheet) {
        return new MatchSheetResponse(
                sheet.getId(),
                sheet.getMatchId(),
                sheet.getFileUrl(),
                sheet.getUploadedBy(),
                sheet.getUploadedAt()
        );
    }
}
