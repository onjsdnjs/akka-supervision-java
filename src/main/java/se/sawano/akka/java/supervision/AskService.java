/*
 * Copyright 2013 Daniel Sawano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.sawano.akka.java.supervision;

import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * An <i>application service</i> providing error reporting when making blocked calls without having to explicitly send a {@link
 * akka.actor.Status.Failure} from the called actor. Without this "high-level" error reporting the callee is forced to be aware of whether
 * the caller is making a blocked call or not. This, in turn, will clutter your code with try-catch blocks and make your design more
 * coupled.
 * <p/>
 * Typical usage:
 * <pre>
 * AskService askService = new AskService(context());
 *
 * ResponseMessage response = askService.ask(Props.create(MyActor.class), new Message(), Timeout.apply(3, SECONDS));
 * </pre>
 * <p/>
 * This is based on the example code provided in the section "HowTo: Common Patterns -- Single-Use Actor Trees with High-Level Error
 * Reporting" section in the Akka documentation.
 */
public class AskService {

    private static class AskSupervisorCreator extends UntypedActor {

        @Override
        public void onReceive(final Object message) throws Exception {
            if (message instanceof AskParam) {
                final ActorRef supervisor = context().actorOf(Props.create(AskSupervisor.class));
                supervisor.forward(message, getContext());
            }
            else {
                unhandled(message);
            }
        }
    }

    private final ActorRef supervisorCreator;

    public AskService(final ActorRefFactory factory) {
        supervisorCreator = createSupervisorCreator(factory);
    }

    public void dispose() {
        supervisorCreator.tell(PoisonPill.getInstance(), null);
    }

    public <T> T ask(final Props props, final Object message, final Timeout timeout) throws Exception {
        final Future<T> future = askOf(supervisorCreator, props, message, timeout);
        return Await.result(future, timeout.duration());
    }

    @SuppressWarnings("unchecked")
    private <T> Future<T> askOf(final ActorRef supervisorCreator, final Props props, final Object message, final Timeout timeout) {
        return (Future<T>) Patterns.ask(supervisorCreator, new AskParam(props, message, timeout), timeout);
    }

    private ActorRef createSupervisorCreator(final ActorRefFactory factory) {
        return factory.actorOf(Props.create(AskSupervisorCreator.class));
    }
}
