package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.request.MatchSheetUploadCommand;
import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchSheet;
import co.edu.escuelaing.techcup.match.exception.MatchSheetAlreadyExistsException;
import co.edu.escuelaing.techcup.match.repository.MatchSheetRepository;
import co.edu.escuelaing.techcup.match.storage.FileStorage;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchSheetServiceImplTest {

    @Mock
    private MatchSheetRepository matchSheetRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private FileStorage fileStorage;

    private MatchSheetServiceImpl matchSheetService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        matchSheetService = new MatchSheetServiceImpl(matchSheetRepository, matchAccessService, fileStorage);

        match = new Match();
        match.setId(matchId);
    }

    @Test
    void uploadSheet_whenNoneExists_storesFileAndPersists() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(matchSheetRepository.findByMatchId(matchId)).thenReturn(Optional.empty());
        when(fileStorage.store(matchId, "acta.pdf", "application/pdf", new byte[]{1, 2, 3}))
                .thenReturn("/storage/match-sheets/" + matchId + "/acta.pdf");
        when(matchSheetRepository.save(any(MatchSheet.class))).thenAnswer(invocation -> {
            MatchSheet sheet = invocation.getArgument(0);
            sheet.setId(UUID.randomUUID());
            sheet.setUploadedAt(Instant.now());
            return sheet;
        });

        MatchSheetResponse response = matchSheetService.uploadSheet(matchId, refereeId,
                new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1, 2, 3}));

        assertThat(response.fileUrl()).contains("acta.pdf");
        assertThat(response.uploadedBy()).isEqualTo(refereeId);
    }

    @Test
    void uploadSheet_whenAlreadyExists_throwsException() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(matchSheetRepository.findByMatchId(matchId)).thenReturn(Optional.of(new MatchSheet()));

        assertThrows(MatchSheetAlreadyExistsException.class, () -> matchSheetService.uploadSheet(
                matchId, refereeId, new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1})));
    }

    @Test
    void uploadSheet_withDisallowedContentType_throwsValidationException() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(matchSheetRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> matchSheetService.uploadSheet(matchId, refereeId,
                new MatchSheetUploadCommand("acta.exe", "application/x-msdownload", new byte[]{1})));
    }

    @Test
    void getSheet_whenNotUploaded_returnsNull() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(matchSheetRepository.findByMatchId(matchId)).thenReturn(Optional.empty());

        assertThat(matchSheetService.getSheet(matchId, refereeId)).isNull();
    }
}
