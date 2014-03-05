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
import akka.japi.Function;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeoutException;

import static akka.actor.Status.Failure;

class AskSupervisor extends UntypedActor {

    private static class AskTimeout {
    }

    private ActorRef targetActor;
    private ActorRef caller;
    private Timeout timeout;
    private Cancellable timeoutMessage;

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>() {
            public SupervisorStrategy.Directive apply(Throwable cause) {
                caller.tell(new Failure(cause), self());
                return SupervisorStrategy.stop();
            }
        });
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        if (message instanceof AskParam) {
            AskParam askParam = (AskParam) message;
            timeout = askParam.timeout;
            caller = sender();
            targetActor = context().actorOf(askParam.props);
            context().watch(targetActor);
            targetActor.forward(askParam.message, context());
            final Scheduler scheduler = context().system().scheduler();
            timeoutMessage = scheduler.scheduleOnce(askParam.timeout.duration(), self(), new AskTimeout(), context().dispatcher(), null);
        }
        else if (message instanceof Terminated) {
            sendFailureToCaller(new ActorKilledException("Target actor terminated."));
            timeoutMessage.cancel();
            context().stop(self());
        }
        else if (message instanceof AskTimeout) {
            sendFailureToCaller(new TimeoutException("Target actor timed out after " + timeout.toString()));
            context().stop(self());
        }
        else {
            unhandled(message);
        }
    }

    private void sendFailureToCaller(final Throwable t) {
        caller.tell(new Failure(t), self());
    }
}