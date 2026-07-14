package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.SubstitutionResponse;
import co.edu.escuelaing.techcup.match.entity.Substitution;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;

public final class SubstitutionMapper {

    private SubstitutionMapper() {
    }

    public static SubstitutionResponse toResponse(Substitution substitution) {
        return new SubstitutionResponse(
                substitution.getId(),
                substitution.getMatchId(),
                substitution.getTeamId(),
                substitution.getPlayerOutId(),
                substitution.getPlayerInId(),
                substitution.getMinute(),
                substitution.getPeriod(),
                EventType.SUBSTITUTION,
                substitution.getCreatedAt()
        );
    }
}
