package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.MatchSheetUploadCommand;
import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.service.MatchSheetService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/partidos/{matchId}/planilla")
@PreAuthorize("@refereeGuard.isReferee()")
public class MatchSheetController {

    private final MatchSheetService matchSheetService;
    private final CurrentRefereeProvider currentRefereeProvider;

    public MatchSheetController(MatchSheetService matchSheetService, CurrentRefereeProvider currentRefereeProvider) {
        this.matchSheetService = matchSheetService;
        this.currentRefereeProvider = currentRefereeProvider;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MatchSheetResponse> uploadSheet(@PathVariable UUID matchId,
                                                            @RequestParam("file") MultipartFile file) {
        try {
            MatchSheetUploadCommand command = new MatchSheetUploadCommand(
                    file.getOriginalFilename(), file.getContentType(), file.getBytes());
            MatchSheetResponse response = matchSheetService.uploadSheet(
                    matchId, currentRefereeProvider.getCurrentRefereeId(), command);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException ex) {
            throw new UncheckedIOException("No fue posible leer el archivo de la planilla", ex);
        }
    }

    @GetMapping
    public ResponseEntity<MatchSheetResponse> getSheet(@PathVariable UUID matchId) {
        MatchSheetResponse response = matchSheetService.getSheet(matchId, currentRefereeProvider.getCurrentRefereeId());
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
}
