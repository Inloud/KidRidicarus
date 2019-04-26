package kidridicarus.game.SMB1.agent.item.powerstar;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.agent.Agent;
import kidridicarus.common.agent.fullactor.FullActorBrain;
import kidridicarus.common.agent.optional.PowerupTakeAgent;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.agentbrain.BrainContactFrameInput;
import kidridicarus.common.agentbrain.BrainFrameInput;
import kidridicarus.common.agentbrain.PowerupBrainContactFrameInput;
import kidridicarus.common.agentbrain.RoomingBrainFrameInput;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.game.SMB1.agent.item.fireflower.SproutSpriteFrameInput;
import kidridicarus.game.SMB1.agent.other.floatingpoints.FloatingPoints;
import kidridicarus.game.info.SMB1_Audio;
import kidridicarus.game.info.SMB1_Pow;

public class PowerStarBrain extends FullActorBrain {
	private static final float SPROUT_TIME = 1f;
	private static final float SPROUT_OFFSET = UInfo.P2M(-13f);
	private static final Vector2 MAX_BOUNCE_VEL = new Vector2(0.5f, 2f); 

	private enum MoveState { SPROUT, WALK, END }

	private PowerStar parent;
	private PowerStarBody body;
	private float moveStateTimer;
	private MoveState moveState;
	private Vector2 initSpawnPosition;
	private PowerupTakeAgent powerupTaker;
	private boolean isFacingRight;
	private boolean isZeroPrevVelY;
	private boolean isBumped;
	private RoomBox lastKnownRoom;
	private Vector2 bumpCenter;
	private boolean despawnMe;

	public PowerStarBrain(PowerStar parent, PowerStarBody body, Vector2 initSpawnPosition) {
		this.parent = parent;
		this.body = body;
		this.initSpawnPosition = initSpawnPosition;
		moveStateTimer = 0f;
		moveState = MoveState.SPROUT;
		powerupTaker = null;
		isFacingRight = true;
		isZeroPrevVelY = false;
		isBumped = false;
		bumpCenter = new Vector2();
		lastKnownRoom = null;
		despawnMe = false;
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
		if(taker.onTakePowerup(new SMB1_Pow.PowerStarPow()))
			powerupTaker = taker;
	}

	@Override
	public SproutSpriteFrameInput processFrame(BrainFrameInput frameInput) {
		processContacts(frameInput);
		return processMove(frameInput.timeDelta);
	}

	private void processContacts(BrainFrameInput frameInput) {
		if(frameInput instanceof RoomingBrainFrameInput) {
			RoomingBrainFrameInput myFrameInput = (RoomingBrainFrameInput) frameInput;
			// if alive and not touching keep alive box, or if touching despawn, then set despawn flag
			if((moveState != MoveState.END && !myFrameInput.isContactKeepAlive) || myFrameInput.isContactDespawn)
				despawnMe = true;
			// update last known room if not used
			if(moveState != MoveState.END && myFrameInput.roomBox != null)
				lastKnownRoom = myFrameInput.roomBox;
		}
	}

	private SproutSpriteFrameInput processMove(float timeDelta) {
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
					body.finishSprout(initSpawnPosition, MAX_BOUNCE_VEL);
				}
				else {
					processBumps();
					processVelocity();
					// do space wrap last so that contacts are maintained
					body.getSpine().checkDoSpaceWrap(lastKnownRoom);
				}
				spritePos.set(body.getPosition());
				break;
			case END:
				// if powerup was taken then play sound and award points
				if(powerupTaker != null) {
					parent.getAgency().getEar().playSound(SMB1_Audio.Sound.POWERUP_USE);
					parent.getAgency().createAgent(FloatingPoints.makeAP(1000, true, body.getPosition(),
							(Agent) powerupTaker));
				}
				parent.getAgency().removeAgent(parent);
				spritePos.set(body.getPosition());
				break;
		}
		moveStateTimer = isMoveStateChange ? 0f : moveStateTimer+timeDelta;
		moveState = nextMoveState;
		return new SproutSpriteFrameInput(true, spritePos, false, timeDelta, finishSprout);
	}

	private MoveState getNextMoveState() {
		if(powerupTaker != null || despawnMe)
			return MoveState.END;
		else if(moveState == MoveState.WALK || (moveState == MoveState.SPROUT && moveStateTimer > SPROUT_TIME))
			return MoveState.WALK;
		else
			return MoveState.SPROUT;
	}

	private void processBumps() {
		// process bumpings
		if(isBumped) {
			isBumped = false;
			// If moving right and bumped from the right then reverse velocity,
			// if moving left and bumped from the left then reverse velocity
			if(isFacingRight && bumpCenter.x > body.getPosition().x)
				isFacingRight = false;
			else if(!isFacingRight && bumpCenter.x < body.getPosition().x)
				isFacingRight = true;
		}
	}

	private void processVelocity() {
		// if horizontal move is blocked by solid and not agent then reverse direction
		if(body.getSpine().isSideMoveBlocked(isFacingRight))
			isFacingRight = !isFacingRight;
		// x velocity is based on facing direction
		float xVel = isFacingRight ? MAX_BOUNCE_VEL.x : -MAX_BOUNCE_VEL.x;
		// clamp +y velocity and maintain constant x velocity
		if(body.getVelocity().y > MAX_BOUNCE_VEL.y)
			body.setVelocity(xVel, MAX_BOUNCE_VEL.y);
		// clamp -y velocity and maintain constant x velocity
		else if(body.getVelocity().y < -MAX_BOUNCE_VEL.y)
			body.setVelocity(xVel, -MAX_BOUNCE_VEL.y);
		// maintain constant x velocity
		else
			body.setVelocity(xVel, body.getVelocity().y);
		// if two consecutive frames of zero Y velocity then apply bounce up velocity
		boolean isZeroVelY = UInfo.epsCheck(body.getVelocity().y, 0f, UInfo.VEL_EPSILON);
		if(isZeroVelY && isZeroPrevVelY)
			body.setVelocity(body.getVelocity().y, MAX_BOUNCE_VEL.y);
		isZeroPrevVelY = isZeroVelY;
	}

	public void onTakeBump(Agent bumpingAgent) {
		// one bump per frame please
		if(isBumped)
			return;
		// if bumping agent doesn't have position then exit
		Vector2 bumpingAgentPos = AP_Tool.getCenter(bumpingAgent);
		if(bumpingAgentPos == null)
			return;

		isBumped = true;
		bumpCenter.set(bumpingAgentPos); 
	}
}