package kidridicarus.game.Metroid.agent.player.samus;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency.AgentHooks;
import kidridicarus.agency.Agent;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentPropertyListener;
import kidridicarus.agency.agent.AgentRemoveCallback;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.optional.ContactDmgTakeAgent;
import kidridicarus.common.agent.optional.PowerupTakeAgent;
import kidridicarus.common.agent.playeragent.PlayerAgent;
import kidridicarus.common.agent.playeragent.PlayerAgentBody;
import kidridicarus.common.agent.playeragent.PlayerAgentSupervisor;
import kidridicarus.common.agent.roombox.RoomBox;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.CommonKV;
import kidridicarus.common.powerup.Powerup;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.common.tool.Direction4;
import kidridicarus.game.Metroid.MetroidKV;
import kidridicarus.game.Metroid.agent.player.samus.HUD.SamusHUD;
import kidridicarus.game.SMB1.agent.HeadBounceGiveAgent;

/*
 * TODO
 * -If Samus has zero energy tanks, then max energy is 99
 * With 1 energy tank, Samus has 199 energy.
 * The extra tank shows as an empty/filled blue square above and on the right side of energy number.
 * When 100 <= energy <= 199, then tank still shows as filled blue square, but when energy drops by 1 to be =99,
 * then energy tank is empty black square.
 * TODO
 * -samus loses JUMPSPIN when her y position goes below her jump start position
 */
public class Samus extends PlayerAgent implements PowerupTakeAgent, ContactDmgTakeAgent, HeadBounceGiveAgent {
	private SamusBrain brain;
	private SamusSprite sprite;
	private SamusHUD playerHUD;

	public Samus(AgentHooks agentHooks, ObjectProperties properties) {
		super(agentHooks, properties);
		body = new SamusBody(this, agentHooks.getWorld(), AP_Tool.getCenter(properties),
				AP_Tool.safeGetVelocity(properties), false);
		brain = new SamusBrain(this, agentHooks, (SamusBody) body,
				properties.getDirection4(CommonKV.KEY_DIRECTION, Direction4.NONE).isRight(),
				properties.getInteger(MetroidKV.KEY_ENERGY_SUPPLY, null));
		sprite = new SamusSprite(agentHooks.getAtlas(), body.getPosition());
		playerHUD = new SamusHUD(this, agentHooks.getAtlas());
		createPropertyListeners();
		agentHooks.addUpdateListener(CommonInfo.UpdateOrder.PRE_MOVE_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) {
					brain.processContactFrame(((SamusBody) body).processContactFrame());
				}
			});
		agentHooks.addUpdateListener(CommonInfo.UpdateOrder.MOVE_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) {
					sprite.processFrame(brain.processFrame(frameTime));
					((PlayerAgentBody) body).resetPrevValues();
				}
			});
		agentHooks.addDrawListener(CommonInfo.DrawOrder.SPRITE_TOP, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { eye.draw(sprite); }
			});
		agentHooks.addDrawListener(CommonInfo.DrawOrder.PLAYER_HUD, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { playerHUD.draw(eye); }
			});
		agentHooks.createAgentRemoveListener(this, new AgentRemoveCallback() {
				@Override
				public void preRemoveAgent() { dispose(); }
			});
	}

	private void createPropertyListeners() {
		agentHooks.addPropertyListener(false, CommonKV.Script.KEY_SPRITE_SIZE,
				new AgentPropertyListener<Vector2>(Vector2.class) {
				@Override
				public Vector2 getValue() { return new Vector2(sprite.getWidth(), sprite.getHeight()); }
			});
		agentHooks.addPropertyListener(false, CommonKV.KEY_DIRECTION,
				new AgentPropertyListener<Direction4>(Direction4.class) {
				@Override
				public Direction4 getValue() { return brain.isFacingRight() ? Direction4.RIGHT : Direction4.LEFT; }
			});
		agentHooks.addPropertyListener(false, MetroidKV.KEY_ENERGY_SUPPLY,
				new AgentPropertyListener<Integer>(Integer.class) {
				@Override
				public Integer getValue() { return brain.getEnergySupply(); }
			});
	}

	@Override
	public boolean onTakePowerup(Powerup pu) {
		return brain.onTakePowerup(pu);
	}

	@Override
	public boolean onTakeDamage(Agent agent, float amount, Vector2 dmgOrigin) {
		return brain.onTakeDamage(amount, dmgOrigin);
	}

	@Override
	public boolean onGiveHeadBounce(Agent agent) {
		return brain.onGiveHeadBounce(agent);
	}

	@Override
	public PlayerAgentSupervisor getSupervisor() {
		return brain.getSupervisor();
	}

	@Override
	public RoomBox getCurrentRoom() {
		return brain.getCurrentRoom();
	}
}
