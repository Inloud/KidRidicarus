package kidridicarus.game.SMB1.agent.item.fireflower;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.agent.Agent;
import kidridicarus.common.agent.halfactor.HalfActorBrain;
import kidridicarus.common.agent.optional.PowerupTakeAgent;
import kidridicarus.common.agentbrain.BrainContactFrameInput;
import kidridicarus.common.agentbrain.PowerupBrainContactFrameInput;
import kidridicarus.common.info.UInfo;
import kidridicarus.game.SMB1.agent.other.floatingpoints.FloatingPoints;
import kidridicarus.game.info.SMB1_Audio;
import kidridicarus.game.info.SMB1_Pow;

public class FireFlowerBrain extends HalfActorBrain {
	private static final float SPROUT_TIME = 1f;
	private static final float SPROUT_OFFSET = UInfo.P2M(-13f);

	private enum MoveState { SPROUT, WALK, END }

	private FireFlower parent;
	private FireFlowerBody body;
	private float moveStateTimer;
	private MoveState moveState;
	private Vector2 initSpawnPosition;
	private PowerupTakeAgent powerupTaker;

	public FireFlowerBrain(FireFlower parent, FireFlowerBody body, Vector2 initSpawnPosition) {
		this.parent = parent;
		this.body = body;
		this.initSpawnPosition = initSpawnPosition;
		moveStateTimer = 0f;
		moveState = MoveState.SPROUT;
		powerupTaker = null;
	}

	public Vector2 getSproutStartPos() {
		return initSpawnPosition.cpy().add(0f, SPROUT_OFFSET);
	}

	@Override
	public void processContactFrame(BrainContactFrameInput cFrameInput) {
		// exit if not finished sprouting or if used
		if(moveState == MoveState.SPROUT || powerupTaker != null)
			return;
		// if any agents touching this powerup are able to take it, then push it to them
		PowerupTakeAgent taker = ((PowerupBrainContactFrameInput) cFrameInput).powerupTaker;
		if(taker == null)
			return;
		if(taker.onTakePowerup(new SMB1_Pow.FireFlowerPow()))
			powerupTaker = taker;
	}


	@Override
	public SproutSpriteFrameInput processFrame(float delta) {
		Vector2 spritePos = new Vector2();
		boolean finishSprout = false;
		MoveState nextMoveState = getNextMoveState();
		boolean isMoveStateChange = nextMoveState != moveState;
		switch(nextMoveState) {
			case SPROUT:
				spritePos.set(initSpawnPosition.cpy().add(0f,
						SPROUT_OFFSET * (SPROUT_TIME - moveStateTimer) / SPROUT_TIME));
				break;
			case WALK:
				if(isMoveStateChange) {
					finishSprout = true;
					body.finishSprout(initSpawnPosition);
				}
				spritePos.set(body.getPosition());
				break;
			case END:
				if(powerupTaker != null) {
					parent.getAgency().getEar().playSound(SMB1_Audio.Sound.POWERUP_USE);
					parent.getAgency().createAgent(FloatingPoints.makeAP(1000, true, body.getPosition(),
							(Agent) powerupTaker));
				}
				parent.getAgency().removeAgent(parent);
				spritePos.set(body.getPosition());
				break;
		}
		moveStateTimer = isMoveStateChange ? 0f : moveStateTimer+delta;
		moveState = nextMoveState;
		return new SproutSpriteFrameInput(true, spritePos, false, delta, finishSprout);
	}

	private MoveState getNextMoveState() {
		if(powerupTaker != null)
			return MoveState.END;
		else if(moveState == MoveState.WALK || (moveState == MoveState.SPROUT && moveStateTimer > SPROUT_TIME))
			return MoveState.WALK;
		else
			return MoveState.SPROUT;
	}
}
