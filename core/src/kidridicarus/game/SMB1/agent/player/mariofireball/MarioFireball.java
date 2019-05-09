package kidridicarus.game.SMB1.agent.player.mariofireball;

import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentRemoveListener;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.general.CorpusAgent;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.CommonKV;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.common.tool.Direction4;
import kidridicarus.game.SMB1.agent.player.mario.Mario;
import kidridicarus.game.info.SMB1_KV;

public class MarioFireball extends CorpusAgent {
	private MarioFireballBrain brain;
	private MarioFireballSprite sprite;

	public MarioFireball(Agency agency, ObjectProperties properties) {
		super(agency, properties);
		boolean isFacingRight;
		// fireball on right?
		if(properties.getDirection4(CommonKV.KEY_DIRECTION, Direction4.NONE).isRight()) {
			isFacingRight = true;
			body = new MarioFireballBody(this, agency.getWorld(), AP_Tool.getCenter(properties),
					MarioFireballSpine.MOVE_VEL.cpy().scl(1, -1));
		}
		// fireball on left
		else {
			isFacingRight = false;
			body = new MarioFireballBody(this, agency.getWorld(), AP_Tool.getCenter(properties),
					MarioFireballSpine.MOVE_VEL.cpy().scl(-1, -1));
		}
		brain = new MarioFireballBrain(this, (MarioFireballBody) body,
				properties.get(CommonKV.KEY_PARENT_AGENT, null, Mario.class), isFacingRight);
		sprite = new MarioFireballSprite(agency.getAtlas(), body.getPosition());
		agency.addAgentUpdateListener(this, CommonInfo.UpdateOrder.PRE_MOVE_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) {
					brain.processContactFrame(((MarioFireballBody) body).processContactFrame());
				}
			});
		agency.addAgentUpdateListener(this, CommonInfo.UpdateOrder.MOVE_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) { sprite.processFrame(brain.processFrame(frameTime)); }
			});
		agency.addAgentDrawListener(this, CommonInfo.DrawOrder.SPRITE_TOPFRONT, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { eye.draw(sprite); }
			});
		agency.addAgentRemoveListener(new AgentRemoveListener(this, this) {
				@Override
				public void preRemoveAgent() { dispose(); }
			});
	}

	public static ObjectProperties makeAP(Vector2 position, boolean right, Mario parentAgent) {
		ObjectProperties props = AP_Tool.createPointAP(SMB1_KV.AgentClassAlias.VAL_MARIOFIREBALL, position);
		props.put(CommonKV.KEY_PARENT_AGENT, parentAgent);
		if(right)
			props.put(CommonKV.KEY_DIRECTION, CommonKV.VAL_RIGHT);
		else
			props.put(CommonKV.KEY_DIRECTION, CommonKV.VAL_LEFT);
		return props;
	}
}
