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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import org.junit.Rule;
import org.junit.Test;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertEquals;
import static se.sawano.akka.java.supervision.FailingActor.FailException;
import static se.sawano.akka.java.supervision.RespondingActor.Message;
import static se.sawano.akka.java.supervision.RespondingActor.Message.HELLO;

public class AskServiceTest extends JavaTestKit {

    @Rule
    public org.junit.rules.Timeout globalTimeout = new org.junit.rules.Timeout((int) MILLISECONDS.convert(1, MINUTES));

    public AskServiceTest() {
        super(ActorSystem.create());
    }

    @Test(expected = TimeoutException.class)
    public void shouldNeverReceiveFailure() throws Exception {
        ActorRef failingActor = getSystem().actorOf(Props.create(FailingActor.class));

        Future<Object> future = Patterns.ask(failingActor, new Object(), akka.util.Timeout.apply(3, SECONDS));
        Await.result(future, Duration.create(3, SECONDS));
    }

    @Test(expected = FailException.class)
    public void shouldReceiveFailure() throws Exception {
        AskService askService = new AskService(getSystem());

        askService.ask(Props.create(FailingActor.class), new Object(), akka.util.Timeout.apply(3, SECONDS));
    }

    @Test
    public void shouldDispose() throws Exception {
        new AskService(getSystem()).dispose();

        Thread.sleep(2000);

        // See log for AskSupervisorCreator being stopped
    }

    @Test
    public void demonstratesTypedUsage() throws Exception {
        AskService askService = new AskService(getSystem());

        Message response = askService.ask(Props.create(RespondingActor.class), new Object(), Timeout.apply(3, SECONDS));

        assertEquals(HELLO, response);

    }
}
