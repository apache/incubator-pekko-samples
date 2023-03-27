package sample.persistence.res.movielist;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.persistence.cassandra.query.javadsl.CassandraReadJournal;
import org.apache.pekko.persistence.typed.ReplicaId;
import org.apache.pekko.persistence.typed.ReplicationId;
import org.apache.pekko.persistence.typed.crdt.ORSet;
import org.apache.pekko.persistence.typed.javadsl.*;

import java.util.Collections;
import java.util.Set;

public final class MovieWatchList
        extends ReplicatedEventSourcedBehavior<MovieWatchList.Command, ORSet.DeltaOp, ORSet<String>> {

    interface Command {}

    public static class AddMovie implements Command {
        public final String movieId;

        public AddMovie(String movieId) {
            this.movieId = movieId;
        }
    }

    public static class RemoveMovie implements Command {
        public final String movieId;

        public RemoveMovie(String movieId) {
            this.movieId = movieId;
        }
    }

    public static class GetMovieList implements Command {
        public final ActorRef<MovieList> replyTo;

        public GetMovieList(ActorRef<MovieList> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class MovieList {
        public final Set<String> movieIds;

        public MovieList(Set<String> movieIds) {
            this.movieIds = Collections.unmodifiableSet(movieIds);
        }
    }

    public static Behavior<Command> create(
            String entityId, ReplicaId replicaId, Set<ReplicaId> allReplicas) {
        return ReplicatedEventSourcing.commonJournalConfig(
                new ReplicationId("movies", entityId, replicaId),
                allReplicas,
                CassandraReadJournal.Identifier(),
                MovieWatchList::new);
    }

    private MovieWatchList(ReplicationContext replicationContext) {
        super(replicationContext);
    }

    @Override
    public ORSet<String> emptyState() {
        return ORSet.empty(getReplicationContext().replicaId());
    }

    @Override
    public CommandHandler<Command, ORSet.DeltaOp, ORSet<String>> commandHandler() {
        return newCommandHandlerBuilder()
                .forAnyState()
                .onCommand(
                        AddMovie.class, (state, command) -> Effect().persist(state.add(command.movieId)))
                .onCommand(
                        RemoveMovie.class,
                        (state, command) -> Effect().persist(state.remove(command.movieId)))
                .onCommand(
                        GetMovieList.class,
                        (state, command) -> {
                            command.replyTo.tell(new MovieList(state.getElements()));
                            return Effect().none();
                        })
                .build();
    }

    @Override
    public EventHandler<ORSet<String>, ORSet.DeltaOp> eventHandler() {
        return newEventHandlerBuilder().forAnyState().onAnyEvent(ORSet::applyOperation);
    }
}

