package kidridicarus.game.agent.SMB1;

import kidridicarus.agency.agent.Agent;

public interface BumpTakeAgent {
	// agent on block when block is jump punched by mario
	public void onTakeBump(Agent bumpingAgent);
}