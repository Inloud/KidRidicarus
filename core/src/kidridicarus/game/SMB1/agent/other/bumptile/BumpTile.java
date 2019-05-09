package kidridicarus.game.SMB1.agent.other.bumptile;

import kidridicarus.agency.Agency;
import kidridicarus.agency.Agent;
import kidridicarus.agency.agent.AgentDrawListener;
import kidridicarus.agency.agent.AgentRemoveListener;
import kidridicarus.agency.agent.AgentUpdateListener;
import kidridicarus.agency.tool.Eye;
import kidridicarus.agency.tool.FrameTime;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.general.CorpusAgent;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.tool.AP_Tool;
import kidridicarus.game.SMB1.agent.TileBumpTakeAgent;
import kidridicarus.game.info.SMB1_KV;

public class BumpTile extends CorpusAgent implements TileBumpTakeAgent {
	private BumpTileBrain brain;
	private BumpTileSprite sprite;

	public BumpTile(Agency agency, ObjectProperties properties) {
		super(agency, properties);
		body = new BumpTileBody(agency.getWorld(), this, AP_Tool.getBounds(properties));
		brain = new BumpTileBrain(this, (BumpTileBody) body,
				properties.getBoolean(SMB1_KV.KEY_SECRETBLOCK, false),
				properties.getString(SMB1_KV.KEY_SPAWNITEM, ""));
		sprite = new BumpTileSprite(agency.getAtlas(), body.getPosition(), AP_Tool.getTexRegion(properties),
				properties.getBoolean(SMB1_KV.KEY_QBLOCK, false));
		agency.addAgentUpdateListener(this, CommonInfo.UpdateOrder.MOVE_UPDATE, new AgentUpdateListener() {
				@Override
				public void update(FrameTime frameTime) { sprite.processFrame(brain.processFrame(frameTime)); }
			});
		agency.addAgentDrawListener(this, CommonInfo.DrawOrder.SPRITE_MIDDLE, new AgentDrawListener() {
				@Override
				public void draw(Eye eye) { eye.draw(sprite); }
			});
		agency.addAgentRemoveListener(new AgentRemoveListener(this, this) {
			@Override
			public void preRemoveAgent() { dispose(); }
		});
	}

	/*
	 * Returns false if tile bump not taken (e.g. because tile is already bumped, etc.).
	 * Otherwise returns true.
	 */
	@Override
	public boolean onTakeTileBump(Agent agent, TileBumpStrength strength) {
		return brain.onTakeTileBump(agent, strength);
	}
}
