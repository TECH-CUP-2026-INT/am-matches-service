package co.edu.escuelaing.techcup.match.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchSheetUploadCommandTest {

    @Test
    void equals_sameContentBytes_areEqual() {
        MatchSheetUploadCommand a = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1, 2, 3});
        MatchSheetUploadCommand b = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentContentBytes_areNotEqual() {
        MatchSheetUploadCommand a = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1, 2, 3});
        MatchSheetUploadCommand b = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{9, 9, 9});

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_sameInstance_isEqual() {
        MatchSheetUploadCommand a = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1});

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equals_differentType_isNotEqual() {
        MatchSheetUploadCommand a = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1});

        assertThat(a).isNotEqualTo("no soy un MatchSheetUploadCommand");
    }

    @Test
    void toString_includesContentBytes() {
        MatchSheetUploadCommand command = new MatchSheetUploadCommand("acta.pdf", "application/pdf", new byte[]{1, 2});

        assertThat(command.toString())
                .contains("acta.pdf")
                .contains("application/pdf")
                .contains("[1, 2]");
    }
}
