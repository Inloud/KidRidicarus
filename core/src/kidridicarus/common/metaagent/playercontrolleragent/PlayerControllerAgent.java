package kidridicarus.common.metaagent.playercontrolleragent;

import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

import kidridicarus.agency.Agency.AgentHooks;
import kidridicarus.agency.Agent;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentRemoveCallback;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.info.AgencyKV;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.agentspawntrigger.AgentSpawnTrigger;
import kidridicarus.common.agent.keepalivebox.KeepAliveBox;
import kidridicarus.common.agent.playeragent.PlayerAgent;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.agent.scrollbox.ScrollBox;
import kidridicarus.common.agent.scrollkillbox.ScrollKillBox;
import kidridicarus.common.agent.scrollpushbox.ScrollPushBox;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.CommonKV;
import kidridicarus.common.info.KeyboardMapping;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.powerup.PowChar;
import kidridicarus.common.powerup.Powerup;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.common.tool.Direction4;
import kidridicarus.common.tool.MoveAdvice4x2;
import kidridicarus.common.tool.QQ;
import kidridicarus.game.KidIcarus.KidIcarusPow;
import kidridicarus.game.Metroid.MetroidPow;
import kidridicarus.game.SMB1.SMB1_Pow;

public class PlayerControllerAgent extends Agent implements Disposable {
	private static final float SPAWN_TRIGGER_WIDTH = UInfo.P2M(UInfo.TILEPIX_X * 20);
	private static final float SPAWN_TRIGGER_HEIGHT = UInfo.P2M(UInfo.TILEPIX_Y * 15);
	private static final float KEEP_ALIVE_WIDTH = UInfo.P2M(UInfo.TILEPIX_X * 22);
	private static final float KEEP_ALIVE_HEIGHT = UInfo.P2M(UInfo.TILEPIX_Y * 15);
	/*
	 * TODO Replace use of safety spawn dist - created because change from small Mario to Samus would sometimes push
	 * new Samus body out of bounds - with a check of nearby space for an empty spot to use as safe spawn position.
	 */
	private static final Vector2 SAFETY_RESPAWN_OFFSET = UInfo.VectorP2M(0f, 8f);

	private PlayerAgent playerAgent;
	private AgentSpawnTrigger spawnTrigger;
	private KeepAliveBox keepAliveBox;
	private ScrollBox scrollBox;
	private MoveAdvice4x2 inputMoveAdvice;
	private Vector2 lastViewCenter;

	public PlayerControllerAgent(AgentHooks agentHooks, ObjectProperties properties) {
		super(agentHooks, properties);

		inputMoveAdvice = new MoveAdvice4x2();
		lastViewCenter = new Vector2(0f, 0f);

		// create the PlayerAgent that this wrapper will control
		ObjectProperties playerAgentProperties =
				properties.get(CommonKV.Player.KEY_AGENT_PROPERTIES, null, ObjectProperties.class);
		createPlayerAgent(playerAgentProperties);

		agentHooks.addUpdateListener(CommonInfo.UpdateOrder.PRE_AGENCY_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) { doPreAgencyUpdate(frameTime); }
			});
		agentHooks.addUpdateListener(CommonInfo.UpdateOrder.POST_AGENCY_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) { doPostAgencyUpdate(); }
			});
		agentHooks.addDrawListener(CommonInfo.DrawOrder.UPDATE_CAMERA, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { updateCamera(); }
			});
		agentHooks.createAgentRemoveListener(this, new AgentRemoveCallback() {
				@Override
				public void preRemoveAgent() { dispose(); }
			});
	}

	private void createPlayerAgent(ObjectProperties playerAgentProperties) {
		// find main player spawner and return fail if none found
		Agent spawner = getMainPlayerSpawner();
		if(spawner == null)
			throw new IllegalStateException("Cannot spawn player, main player spawner not found.");

		// spawn player with properties at spawn location
		playerAgent = spawnPlayerAgentWithProperties(playerAgentProperties, spawner);
		// create player's associated agents (generally, they follow player)
		spawnTrigger = (AgentSpawnTrigger) agentHooks.createAgent(
				AgentSpawnTrigger.makeAP(getViewCenter(), SPAWN_TRIGGER_WIDTH, SPAWN_TRIGGER_HEIGHT));
		keepAliveBox = (KeepAliveBox) agentHooks.createAgent(
				KeepAliveBox.makeAP(getViewCenter(), KEEP_ALIVE_WIDTH, KEEP_ALIVE_HEIGHT));
	}

	// get user input
	private void doPreAgencyUpdate(FrameTime frameTime) {
		if(playerAgent == null)
			return;
		// ensure spawn trigger and keep alive box follow view center
		spawnTrigger.setTarget(getViewCenter());
		keepAliveBox.setTarget(getViewCenter());
		if(scrollBox != null)
			scrollBox.setTarget(getViewCenter());
		handleInput();
		playerAgent.getSupervisor().preUpdateAgency(frameTime);
	}

	private void handleInput() {
		if(playerAgent == null)
			return;

		inputMoveAdvice.moveRight = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_RIGHT);
		inputMoveAdvice.moveUp = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_UP);
		inputMoveAdvice.moveLeft = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_LEFT);
		inputMoveAdvice.moveDown = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_DOWN);
		inputMoveAdvice.action0 = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_RUNSHOOT);
		inputMoveAdvice.action1 = Gdx.input.isKeyPressed(KeyboardMapping.MOVE_JUMP);

		if(Gdx.input.isKeyJustPressed(KeyboardMapping.DEBUG_TOGGLE))
			QQ.toggleOn();
		if(Gdx.input.isKeyJustPressed(KeyboardMapping.CHEAT_POWERUP_MARIO))
			Powerup.tryPushPowerup(playerAgent, new SMB1_Pow.FireFlowerPow());
		else if(Gdx.input.isKeyJustPressed(KeyboardMapping.CHEAT_POWERUP_SAMUS))
			Powerup.tryPushPowerup(playerAgent, new MetroidPow.EnergyPow());
		else if(Gdx.input.isKeyJustPressed(KeyboardMapping.CHEAT_POWERUP_PIT))
			Powerup.tryPushPowerup(playerAgent, new KidIcarusPow.AngelHeartPow(5));

		// pass user input to player agent's supervisor
		playerAgent.getSupervisor().setMoveAdvice(inputMoveAdvice);
	}

	private void doPostAgencyUpdate() {
		if(playerAgent == null)
			return;

		// check for "out-of-character" powerup received and change to appropriate character for powerup
		Powerup nonCharPowerup = playerAgent.getSupervisor().getNonCharPowerups().getFirst();
		playerAgent.getSupervisor().clearNonCharPowerups();
		if(nonCharPowerup != null)
			switchAgentType(nonCharPowerup.getPowerupCharacter());

		playerAgent.getSupervisor().postUpdateAgency();
		checkCreateScrollBox();
	}

	/*
	 * As the player moves into and out of rooms, the scroll box may need to be created / removed / changed.
	 */
	private void checkCreateScrollBox() {
		RoomBox currentRoom = playerAgent.getCurrentRoom();
		if(currentRoom == null)
			return;
		Direction4 scrollDir =
				currentRoom.getProperty(CommonKV.Room.KEY_SCROLL_DIR, Direction4.NONE, Direction4.class);
		// if current room has scroll push box property = true then create/change to scroll push box
		if(currentRoom.getProperty(CommonKV.Room.KEY_SCROLL_PUSHBOX, false, Boolean.class)) {
			if(scrollBox != null && !(scrollBox instanceof ScrollPushBox)) {
				scrollBox.removeSelf();
				scrollBox = null;
			}
			// if scroll box needs to be created and a valid scroll direction is given then create push box
			if(scrollBox == null && scrollDir != Direction4.NONE)
				scrollBox = (ScrollPushBox) agentHooks.createAgent(ScrollPushBox.makeAP(getViewCenter(), scrollDir));
		}
		// if current room has scroll kill box property = true then create/change to scroll kill box
		else if(currentRoom.getProperty(CommonKV.Room.KEY_SCROLL_KILLBOX, false, Boolean.class)) {
			if(scrollBox != null && !(scrollBox instanceof ScrollKillBox)) {
				scrollBox.removeSelf();
				scrollBox = null;
			}
			// if scroll box needs to be created and a valid scroll direction is given then create kill box
			if(scrollBox == null && scrollDir != Direction4.NONE)
				scrollBox = (ScrollKillBox) agentHooks.createAgent(ScrollKillBox.makeAP(getViewCenter(), scrollDir));
		}
		// need to remove a scroll box?
		else if(scrollBox != null) {
			scrollBox.removeSelf();
			scrollBox = null;
		}
	}

	private void switchAgentType(PowChar pc) {
		// if power character class alias is blank then throw exception
		if(pc.getAgentClassAlias().equals(""))
			throw new IllegalArgumentException("Cannot create player Agent from blank class alias.");
		// if player Agent is null or doesn't have position then throw exception
		if(playerAgent == null)
			throw new IllegalStateException("Current player Agent cannot be null when switching power character.");

		// save copy of position
		Vector2 oldPosition = AP_Tool.getCenter(playerAgent);
		if(oldPosition == null) {
			throw new IllegalStateException(
					"Current player Agent must have a position when switching power character.");
		}
		// save copy of facing right
		boolean facingRight = AP_Tool.safeGetDirection4(playerAgent).isRight();
		// save copy of velocity
		Vector2 oldVelocity = AP_Tool.getVelocity(playerAgent);
		// remove old player character
		playerAgent.removeSelf();
		playerAgent = null;
		// create new player character properties
		ObjectProperties props = AP_Tool.createPointAP(pc.getAgentClassAlias(),
				oldPosition.cpy().add(SAFETY_RESPAWN_OFFSET));
		// put facing right property if needed
		if(facingRight)
			props.put(CommonKV.KEY_DIRECTION, Direction4.RIGHT);
		// put velocity if available
		if(oldVelocity != null)
			props.put(CommonKV.KEY_VELOCITY, oldVelocity);
		// create new player power character Agent
		playerAgent = (PlayerAgent) agentHooks.createAgent(props);
	}

	private void updateCamera() {
		if(playerAgent == null)
			return;
		// if player is not dead then use their current room to determine the gamecam position
		if(!playerAgent.getSupervisor().isGameOver())
			agentHooks.getEye().setViewCenter(getViewCenter());
	}

	private Agent getMainPlayerSpawner() {
		// find main spawnpoint and spawn player there, or spawn at (0, 0) if no spawnpoint found
		LinkedList<Agent> spawnList = agentHooks.getAgentsByProperties(
				new String[] { AgencyKV.KEY_AGENT_CLASS, CommonKV.Spawn.KEY_SPAWN_MAIN },
				new Object[] { CommonKV.AgentClassAlias.VAL_PLAYER_SPAWNER, true });
		if(spawnList.isEmpty())
			return null;
		return spawnList.getFirst();
	}

	// TODO Refactor the following two methods (spawnPlayerAgentWithProperties,
	// spawnPlayerAgentWithSpawnerProperties) to simplify.
	private PlayerAgent spawnPlayerAgentWithProperties(ObjectProperties playerAgentProperties, Agent spawner) {
		// if no spawn position then return null
		Vector2 spawnPos = AP_Tool.getCenter(spawner);
		if(spawnPos == null)
			return null;
		// if no agent properties given then use spawner to determine player class and position
		if(playerAgentProperties == null)
			return spawnPlayerAgentWithSpawnerProperties(spawner, spawnPos);
		// otherwise use agent properties and set start point to main spawn point
		else {
			// otherwise insert spawn position into properties and create player Agent
			playerAgentProperties.put(CommonKV.KEY_POSITION, spawnPos);
			return (PlayerAgent) agentHooks.createAgent(playerAgentProperties);
		}
	}

	private PlayerAgent spawnPlayerAgentWithSpawnerProperties(Agent spawner, Vector2 spawnPos) {
		String initPlayClass = spawner.getProperty(CommonKV.Spawn.KEY_PLAYER_AGENTCLASS, null, String.class);
		if(initPlayClass == null)
			return null;
		ObjectProperties playerAP = AP_Tool.createPointAP(initPlayClass, spawnPos);
		if(AP_Tool.safeGetDirection4(spawner).isRight())
			playerAP.put(CommonKV.KEY_DIRECTION, Direction4.RIGHT);
		return (PlayerAgent) agentHooks.createAgent(playerAP);
	}

	// safely get the view center - cannot return null, and tries to return a correct view center
	private Vector2 getViewCenter() {
		Vector2 vc = null;
		if(playerAgent != null)
			vc = playerAgent.getSupervisor().getViewCenter();
		if(vc == null)
			vc = lastViewCenter;
		else
			lastViewCenter.set(vc);
		return vc;
	}

	public boolean isGameWon() {
		if(playerAgent == null)
			return false;
		return playerAgent.getSupervisor().isAtLevelEnd();
	}

	public boolean isGameOver() {
		if(playerAgent == null)
			return false;
		return playerAgent.getSupervisor().isGameOver();
	}

	public String getNextLevelFilename() {
		if(playerAgent == null)
			return null;
		return playerAgent.getSupervisor().getNextLevelFilename();
	}

	public ObjectProperties getCopyPlayerAgentProperties() {
		if(playerAgent == null)
			return null;
		return playerAgent.getAllProperties();
	}

	@Override
	public void dispose() {
		if(scrollBox != null)
			scrollBox.dispose();
		if(keepAliveBox != null)
			keepAliveBox.dispose();
		if(spawnTrigger != null)
			spawnTrigger.dispose();
		if(playerAgent != null)
			playerAgent.dispose();
	}

	public static ObjectProperties makeAP(ObjectProperties playerAgentProperties) {
		ObjectProperties props = AP_Tool.createAP(CommonKV.AgentClassAlias.VAL_PLAYER_CONTROLLER);
		props.put(CommonKV.Player.KEY_AGENT_PROPERTIES, playerAgentProperties);
		return props;
	}
}
