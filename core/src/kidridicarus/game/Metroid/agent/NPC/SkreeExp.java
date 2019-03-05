package kidridicarus.game.Metroid.agent.NPC;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.DrawableAgent;
import kidridicarus.agency.agent.UpdatableAgent;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.optional.ContactDmgGiveAgent;
import kidridicarus.game.Metroid.agentbody.NPC.SkreeExpBody;
import kidridicarus.game.Metroid.agentsprite.NPC.SkreeExpSprite;
import kidridicarus.game.info.GfxInfo;

public class SkreeExp extends Agent implements UpdatableAgent, DrawableAgent, ContactDmgGiveAgent {
	private static final float LIVE_TIME = 0.167f;

	private SkreeExpBody seBody;
	private SkreeExpSprite seSprite;
	private float stateTimer;

	public SkreeExp(Agency agency, ObjectProperties properties) {
		super(agency, properties);
		stateTimer = 0f;
		seBody = new SkreeExpBody(this, agency.getWorld(), Agent.getStartPoint(properties),
				Agent.getStartVelocity(properties));
		seSprite = new SkreeExpSprite(agency.getAtlas(), seBody.getPosition());
		agency.enableAgentUpdate(this);
		agency.setAgentDrawOrder(this, GfxInfo.LayerDrawOrder.SPRITE_BOTTOM);
	}

	@Override
	public void update(float delta) {
		if(stateTimer > LIVE_TIME) {
			agency.disposeAgent(this);
		}
		else {
			seSprite.update(seBody.getPosition());
			stateTimer += delta;
		}
	}

	@Override
	public void draw(Batch batch) {
		seSprite.draw(batch);
	}

	@Override
	public Vector2 getPosition() {
		return seBody.getPosition();
	}

	@Override
	public Rectangle getBounds() {
		return seBody.getBounds();
	}

	@Override
	public boolean isContactDamage() {
		return true;
	}

	@Override
	public void dispose() {
		seBody.dispose();
	}
}
