package kidridicarus.game.KidIcarus.agent.other.kidicarusdoor;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.Agent;
import kidridicarus.common.agent.halfactor.HalfActorBrain;
import kidridicarus.common.agent.playeragent.PlayerAgent;
import kidridicarus.common.agent.playerspawner.PlayerSpawner;
import kidridicarus.common.agentbrain.BrainContactFrameInput;

public class KidIcarusDoorBrain extends HalfActorBrain {
	private KidIcarusDoor parent;
	private KidIcarusDoorBody body;
	private boolean isOpened;
	private String exitSpawnerName;

	public KidIcarusDoorBrain(KidIcarusDoor parent, KidIcarusDoorBody body, boolean isOpened,
			String exitSpawnerName) {
		this.parent = parent;
		this.body = body;
		this.isOpened = isOpened;
		this.exitSpawnerName = exitSpawnerName;
	}

	@Override
	public void processContactFrame(BrainContactFrameInput cFrameInput) {
		// if not opened then door cannot be used, so exit
		if(!isOpened)
			return;
		// if the exit spawner is the wrong class then exit this method
		Agent exitSpawner = Agency.getTargetAgent(parent.getAgency(), exitSpawnerName);
		if(!(exitSpawner instanceof PlayerSpawner)) {
			throw new IllegalArgumentException("Exit spawner is not instance of "+PlayerSpawner.class.getName()+
					", exitSpawner="+exitSpawnerName);
		}
		// check for players touching door and pass them door script
		for(PlayerAgent agent : ((KidIcarusDoorBrainContactFrameInput) cFrameInput).playerContacts)
			agent.getSupervisor().startScript(new KidIcarusDoorScript(parent, exitSpawner));
	}

	@Override
	public KidIcarusDoorSpriteFrameInput processFrame(float delta) {
		body.setOpened(isOpened);
		return new KidIcarusDoorSpriteFrameInput(body.getPosition(), isOpened);
	}

	public void setOpened(boolean isOpened) {
		this.isOpened = isOpened;
	}
}
