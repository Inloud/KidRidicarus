package kidridicarus.game.agent.SMB.item.mushroom;

import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.common.agent.optional.PowerupTakeAgent;
import kidridicarus.common.agentsensor.AgentContactHoldSensor;
import kidridicarus.common.agentsensor.SolidBoundSensor;
import kidridicarus.common.agentspine.OnGroundSpine;
import kidridicarus.common.agentspine.PowerupSpine;

public class WalkPowerupSpine extends OnGroundSpine {
	private AgentBody body;
	private PowerupSpine powerupSpine;
	private SolidBoundSensor hmSensor;

	public WalkPowerupSpine(AgentBody body) {
		this.body = body;
		powerupSpine = new PowerupSpine(body);
		hmSensor = null;
	}

	public AgentContactHoldSensor createAgentSensor() {
		return powerupSpine.createAgentSensor();
	}

	public PowerupTakeAgent getTouchingPowerupTaker() {
		return powerupSpine.getTouchingPowerupTaker();
	}

	public SolidBoundSensor createHMSensor() {
		hmSensor = new SolidBoundSensor(body);
		return hmSensor;
	}

	public boolean isHMoveBlocked(boolean moveRight) {
		return hmSensor.isHMoveBlocked(body.getBounds(), moveRight);
	}
}
