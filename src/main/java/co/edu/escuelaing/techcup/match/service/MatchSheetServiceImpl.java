package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.MatchSheetUploadCommand;
import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchSheet;
import co.edu.escuelaing.techcup.match.exception.MatchSheetAlreadyExistsException;
import co.edu.escuelaing.techcup.match.mapper.MatchSheetMapper;
import co.edu.escuelaing.techcup.match.repository.MatchSheetRepository;
import co.edu.escuelaing.techcup.match.storage.FileStorage;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MatchSheetServiceImpl implements MatchSheetService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");

    private final MatchSheetRepository matchSheetRepository;
    private final MatchAccessService matchAccessService;
    private final FileStorage fileStorage;

    public MatchSheetServiceImpl(MatchSheetRepository matchSheetRepository,
                                  MatchAccessService matchAccessService,
                                  FileStorage fileStorage) {
        this.matchSheetRepository = matchSheetRepository;
        this.matchAccessService = matchAccessService;
        this.fileStorage = fileStorage;
    }

    @Override
    public MatchSheetResponse uploadSheet(UUID matchId, UUID refereeId, MatchSheetUploadCommand command) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);

        if (matchSheetRepository.findByMatchId(matchId).isPresent()) {
            throw new MatchSheetAlreadyExistsException(matchId);
        }
        if (!ALLOWED_CONTENT_TYPES.contains(command.contentType())) {
            throw new ValidationException(
                    "Formato de archivo no permitido para la planilla. Use PDF, JPEG o PNG.");
        }

        String fileUrl = fileStorage.store(matchId, command.fileName(), command.contentType(), command.content());

        MatchSheet sheet = new MatchSheet();
        sheet.setMatchId(match.getId());
        sheet.setFileUrl(fileUrl);
        sheet.setUploadedBy(refereeId);
        sheet.setUploadedAt(Instant.now());
        sheet = matchSheetRepository.save(sheet);

        return MatchSheetMapper.toResponse(sheet);
    }

    @Override
    public MatchSheetResponse getSheet(UUID matchId, UUID refereeId) {
        matchAccessService.requireOwnedMatch(matchId, refereeId);
        return matchSheetRepository.findByMatchId(matchId)
                .map(MatchSheetMapper::toResponse)
                .orElse(null);
    }
}
