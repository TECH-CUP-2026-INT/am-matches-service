package co.edu.escuelaing.techcup.match.messaging;

public interface MatchFinishedEventPublisher {

    void publish(MatchFinishedEvent event);
}
