package kidridicarus.game.SMB.agent.item;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.DisposableAgent;
import kidridicarus.agency.agent.DrawableAgent;
import kidridicarus.agency.agent.UpdatableAgent;
import kidridicarus.agency.tool.ObjectProperties;
import kidridicarus.common.agent.optional.PowerupGiveAgent;
import kidridicarus.common.info.CommonInfo;
import kidridicarus.common.info.GfxInfo;
import kidridicarus.common.info.UInfo;
import kidridicarus.game.SMB.agent.player.Mario;
import kidridicarus.game.SMB.agentbody.item.FireFlowerBody;
import kidridicarus.game.SMB.agentsprite.item.FireFlowerSprite;
import kidridicarus.game.info.PowerupInfo.PowType;

public class FireFlower extends Agent implements UpdatableAgent, DrawableAgent, PowerupGiveAgent,
		DisposableAgent {
	private static final float SPROUT_TIME = 1f;
	private static final float SPROUT_OFFSET = UInfo.P2M(-13f);

	private FireFlowerSprite flowerSprite;
	private FireFlowerBody ffBody;
	private float stateTimer;
	private boolean isSprouting;
	private Vector2 sproutingPosition;

	public FireFlower(Agency agency, ObjectProperties properties) {
		super(agency, properties);

		sproutingPosition = Agent.getStartPoint(properties);
		flowerSprite = new FireFlowerSprite(agency.getAtlas(), sproutingPosition.cpy().add(0f, SPROUT_OFFSET));

		stateTimer = 0f;
		isSprouting = true;
		agency.setAgentUpdateOrder(this, CommonInfo.AgentUpdateOrder.UPDATE);
		agency.setAgentDrawOrder(this, GfxInfo.LayerDrawOrder.SPRITE_BOTTOM);
	}

	@Override
	public void update(float delta) {
		processSprite(delta);
	}

	private void processSprite(float delta) {
		if(isSprouting) {
			float yOffset = 0f;
			if(stateTimer > SPROUT_TIME) {
				isSprouting = false;
				agency.setAgentDrawOrder(this, GfxInfo.LayerDrawOrder.SPRITE_MIDDLE);
				ffBody = new FireFlowerBody(this, agency.getWorld(), sproutingPosition);
			}
			else
				yOffset = SPROUT_OFFSET * (SPROUT_TIME - stateTimer) / SPROUT_TIME;

			flowerSprite.update(delta, sproutingPosition.cpy().add(0f, yOffset));
		}
		else
			flowerSprite.update(delta, ffBody.getPosition());

		// increment state timer
		stateTimer += delta;
	}

	@Override
	public void draw(Batch batch){
		flowerSprite.draw(batch);
	}

	@Override
	public void use(Agent agent) {
		if(stateTimer > SPROUT_TIME && agent instanceof Mario) {
			((Mario) agent).applyPowerup(PowType.FIREFLOWER);
			agency.disposeAgent(this);
		}
	}

	@Override
	public Vector2 getPosition() {
		return ffBody.getPosition();
	}

	@Override
	public Rectangle getBounds() {
		return ffBody.getBounds();
	}

	@Override
	public void disposeAgent() {
		ffBody.dispose();
	}
}