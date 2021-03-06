package kidridicarus.game.SMB1.agent.other.castleflag;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency.AgentHooks;
import kidridicarus.agency.Agent;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.agentsprite.SpriteFrameInput;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.optional.TriggerTakeAgent;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.common.tool.SprFrameTool;

public class CastleFlag extends Agent implements TriggerTakeAgent {
	private static final float RISE_DIST = UInfo.P2M(32);
	private static final float RISE_TIME = 1f;

	private enum MoveState { DOWN, RISING, UP}

	private AgentUpdateListener myUpdateListener;
	private CastleFlagSprite sprite;
	private Vector2 startPosition;
	private boolean isTriggered;
	private MoveState curMoveState;
	private float stateTimer;

	public CastleFlag(AgentHooks agentHooks, ObjectProperties properties) {
		super(agentHooks, properties);
		curMoveState = MoveState.DOWN;
		stateTimer = 0f;
		myUpdateListener = null;
		startPosition = AP_Tool.getCenter(properties);
		isTriggered = false;
		sprite = new CastleFlagSprite(agentHooks.getAtlas(), startPosition);
		agentHooks.addDrawListener(CommonInfo.DrawOrder.SPRITE_BOTTOM, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { eye.draw(sprite); }
			});
	}

	private SpriteFrameInput processFrame(FrameTime frameTime) {
		float yOffset;
		MoveState nextMoveState = getNextMoveState();
		switch(nextMoveState) {
			case DOWN:
			default:
				yOffset = 0f;
				if(isTriggered)
					curMoveState = MoveState.RISING;
				break;
			case RISING:
				if(curMoveState != nextMoveState)
					yOffset = 0f;
				else
					yOffset = RISE_DIST / RISE_TIME * stateTimer;
				break;
			case UP:
				yOffset = RISE_DIST;
				// disable updates
				agentHooks.removeUpdateListener(myUpdateListener);
				break;
		}
		stateTimer = curMoveState != nextMoveState ? 0f : stateTimer+frameTime.timeDelta;
		curMoveState = nextMoveState;
		return SprFrameTool.place(startPosition.cpy().add(0f, yOffset));
	}

	private MoveState getNextMoveState() {
		switch(curMoveState) {
			case DOWN:
			default:
				if(isTriggered)
					return MoveState.RISING;
				return MoveState.DOWN;
			case RISING:
				if(stateTimer > RISE_TIME)
					return MoveState.UP;
				return MoveState.RISING;
			case UP:
				return MoveState.UP;
		}
	}

	@Override
	public void onTakeTrigger() {
		isTriggered = true;
		// enable updates
		myUpdateListener = new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) { sprite.processFrame(processFrame(frameTime)); }
			};
		agentHooks.addUpdateListener(CommonInfo.UpdateOrder.MOVE_UPDATE, myUpdateListener);
	}
}
