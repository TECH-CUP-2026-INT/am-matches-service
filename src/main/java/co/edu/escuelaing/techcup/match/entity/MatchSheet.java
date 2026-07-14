package co.edu.escuelaing.techcup.match.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "match_sheet")
@Getter
@Setter
@NoArgsConstructor
public class MatchSheet {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed(unique = true)
    private UUID matchId;

    private String fileUrl;

    private UUID uploadedBy;

    private Instant uploadedAt;
}
