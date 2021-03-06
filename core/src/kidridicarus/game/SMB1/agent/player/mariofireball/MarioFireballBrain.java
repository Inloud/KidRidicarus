package kidridicarus.game.SMB1.agent.player.mariofireball;

import kidridicarus.agency.Agency.AgentHooks;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.common.agent.optional.ContactDmgTakeAgent;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.agentbrain.BrainContactFrameInput;
import kidridicarus.common.agentbrain.ContactDmgBrainContactFrameInput;
import kidridicarus.game.SMB1.SMB1_Audio;
import kidridicarus.game.SMB1.agent.player.mario.Mario;

class MarioFireballBrain {
	private static final float DAMAGE = 1f;
	private static final float EXPLODE_TIME = 1/10f;

	enum MoveState { FLY, EXPLODE, END }
	private enum HitType { NONE, BOUNDARY, AGENT }

	private Mario playerParent;
	private AgentHooks parentHooks;
	private MarioFireballBody body;
	private float moveStateTimer;
	private MoveState moveState;
	private boolean isFacingRight;
	private HitType hitType;
	private RoomBox lastKnownRoom;

	MarioFireballBrain(Mario playerParent, AgentHooks parentHooks, MarioFireballBody body,
			boolean isFacingRight) {
		this.playerParent = playerParent;
		this.parentHooks = parentHooks;
		this.body = body;
		this.isFacingRight = isFacingRight;
		moveStateTimer = 0f;
		moveState = MoveState.FLY;
		hitType = HitType.NONE;
		lastKnownRoom = null;
	}

	void processContactFrame(BrainContactFrameInput cFrameInput) {
		// check do agents needing damage
		for(ContactDmgTakeAgent agent : ((ContactDmgBrainContactFrameInput) cFrameInput).contactDmgTakeAgents) {
			// if contact agent took damage then set hit Agent flag
			if(agent != playerParent && agent.onTakeDamage(playerParent, DAMAGE, body.getPosition())) {
				hitType = HitType.AGENT;
				break;
			}
		}

		// if alive and not touching keep alive box, or if touching despawn, or if hit a solid, then explode
		if(!cFrameInput.isKeepAlive || cFrameInput.isDespawn)
			hitType = HitType.BOUNDARY;
		// otherwise update last known room if possible
		else if(cFrameInput.room != null)
			lastKnownRoom = cFrameInput.room;

		// check for hits against solids
		if(hitType == HitType.NONE && body.getSpine().isHitBoundary(isFacingRight))
			hitType = HitType.BOUNDARY;
	}

	MarioFireballSpriteFrame processFrame(FrameTime frameTime) {
		MoveState nextMoveState = getNextMoveState();
		boolean isMoveStateChange = nextMoveState != moveState;
		switch(nextMoveState) {
			case FLY:
				// check for bounce (y velocity) and maintain x velocity
				body.getSpine().doVelocityCheck();
				break;
			case EXPLODE:
				if(isMoveStateChange) {
					body.getSpine().startExplode();
					// play sound for hit agent or play sound for hit boundary line
					if(hitType == HitType.AGENT)
						parentHooks.getEar().playSound(SMB1_Audio.Sound.KICK);
					else
						parentHooks.getEar().playSound(SMB1_Audio.Sound.BUMP);
				}
				break;
			case END:
				parentHooks.removeThisAgent();
				break;
		}
		// do space wrap last so that contacts are maintained (e.g. keep alive box contact)
		body.getSpine().checkDoSpaceWrap(lastKnownRoom);
		moveStateTimer = isMoveStateChange ? 0f : moveStateTimer+frameTime.timeDelta;
		moveState = nextMoveState;

		body.postUpdate();

		return new MarioFireballSpriteFrame(body.getPosition(), isFacingRight, frameTime, moveState);
	}

	private MoveState getNextMoveState() {
		if(moveState == MoveState.EXPLODE) {
			if(moveStateTimer > EXPLODE_TIME)
				return MoveState.END;
			else
				return MoveState.EXPLODE;
		}
		else if(hitType != HitType.NONE)
			return MoveState.EXPLODE;
		else
			return MoveState.FLY;
	}
}
