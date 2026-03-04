package com.comicsai.ai.agent;

import com.comicsai.ai.message.Msg;

/**
 * Core Agent abstraction inspired by AgentScope.
 * An Agent receives a Msg, performs reasoning (prompt engineering + model call),
 * and returns a response Msg.
 */
public interface Agent {

    /**
     * Process an input message and produce a response.
     *
     * @param input the incoming message (context, instructions, etc.)
     * @return the agent's response
     */
    Msg call(Msg input);

    /** Agent identity used for logging and message attribution. */
    String getName();
}
