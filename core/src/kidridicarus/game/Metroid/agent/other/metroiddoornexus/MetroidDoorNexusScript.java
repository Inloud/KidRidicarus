package kidridicarus.game.Metroid.agent.other.metroiddoornexus;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agentscript.AgentScript;
import kidridicarus.agency.agentscript.ScriptedAgentState;
import kidridicarus.agency.agentscript.ScriptedSpriteState.SpriteState;
import kidridicarus.game.Metroid.agent.other.metroiddoor.MetroidDoor;

public class MetroidDoorNexusScript implements AgentScript {
	private static final float TRANSIT_TIME = 3f;

	private enum ScriptState { FIRST_HALF, SECOND_HALF, COMPLETE }

	private ScriptedAgentState beginAgentState;
	private ScriptedAgentState curScriptAgentState;
	private float stateTimer;
	private ScriptState curScriptState;
	private MetroidDoor triggerDoor;
	private float exitPositionX;
	private float exitPositionY;

	public MetroidDoorNexusScript(MetroidDoorNexus parent, boolean isTransitRight, MetroidDoor leftDoor,
			MetroidDoor rightDoor, Vector2 incomingAgentSize) {
		if(isTransitRight)
			triggerDoor = rightDoor;
		else
			triggerDoor = leftDoor;
		// get right door exit position if transiting right
		if(isTransitRight) {
			if(rightDoor == null)
				exitPositionX = parent.getPosition().x + parent.getBounds().width/2f + incomingAgentSize.x/4f;
			else
				exitPositionX = rightDoor.getPosition().x + rightDoor.getBounds().width/2f + incomingAgentSize.x/4f;
		}
		// otherwise get left door exit position
		else {
			if(leftDoor == null)
				exitPositionX = parent.getPosition().x - parent.getBounds().width/2f - incomingAgentSize.x/4f;
			else
				exitPositionX = leftDoor.getPosition().x - leftDoor.getBounds().width/2f - incomingAgentSize.x/4f;
		}
		// exit position Y is dependent upon player position Y, so it is set in startScript method
		exitPositionY = 0f;

		beginAgentState = null;
		curScriptAgentState = null;
		stateTimer = 0f;
		curScriptState = ScriptState.FIRST_HALF;
	}

	@Override
	public void startScript(Agency agency, AgentScriptHooks asHooks, ScriptedAgentState beginScriptAgentState) {
		this.beginAgentState = beginScriptAgentState.cpy();
		this.curScriptAgentState = beginScriptAgentState.cpy();

		curScriptAgentState.scriptedBodyState.contactEnabled = false;
		curScriptAgentState.scriptedBodyState.gravityFactor = 0f;

		exitPositionY = beginAgentState.scriptedBodyState.position.y;
	}

	@Override
	public boolean update(float delta) {
		ScriptState nextScriptState = getNextScriptState();
		boolean scriptStateChanged = nextScriptState != curScriptState;
		switch(nextScriptState) {
			case FIRST_HALF:
				curScriptAgentState.scriptedSpriteState.position.set(getSpritePosition(stateTimer / TRANSIT_TIME));
				curScriptAgentState.scriptedSpriteState.spriteState = SpriteState.STAND;
				break;
			case SECOND_HALF:
				// if first frame of second half then set body position to exit position
				if(scriptStateChanged) {
					curScriptAgentState.scriptedBodyState.position.set(exitPositionX, exitPositionY);
					triggerDoor.onTakeTrigger();
				}

				// sprite position animates from player entry position to nexus exit position
				curScriptAgentState.scriptedSpriteState.position.set(
						getSpritePosition(0.5f + stateTimer / TRANSIT_TIME));
				curScriptAgentState.scriptedSpriteState.spriteState = SpriteState.STAND;
				break;
			// script complete, return false to end script
			case COMPLETE:
				// contacts and gravity were disabled at the start of script, so re-enable here
				curScriptAgentState.scriptedBodyState.contactEnabled = true;
				curScriptAgentState.scriptedBodyState.gravityFactor = 1f;
				return false;
		}

		stateTimer = curScriptState == nextScriptState ? stateTimer+delta : 0f;
		curScriptState = nextScriptState;
		return true;
	}

	private ScriptState getNextScriptState() {
		if(curScriptState == ScriptState.COMPLETE)
			return ScriptState.COMPLETE;
		else if(curScriptState == ScriptState.SECOND_HALF) {
			if(stateTimer > TRANSIT_TIME/2f)
				return ScriptState.COMPLETE;
			else
				return ScriptState.SECOND_HALF;
		}
		else if(curScriptState == ScriptState.FIRST_HALF && stateTimer > TRANSIT_TIME/2f)
			return ScriptState.SECOND_HALF;
		else
			return ScriptState.FIRST_HALF;
	}

	private Vector2 getSpritePosition(float lerpVal) {
		return beginAgentState.scriptedBodyState.position.cpy().lerp(new Vector2(exitPositionX, exitPositionY),
				lerpVal);
	}

	@Override
	public ScriptedAgentState getScriptAgentState() {
		return curScriptAgentState;
	}

	@Override
	public boolean isOverridable(AgentScript nextScript) {
		return false;
	}
}