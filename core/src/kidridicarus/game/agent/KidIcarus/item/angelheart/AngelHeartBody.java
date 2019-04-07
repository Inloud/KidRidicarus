package kidridicarus.game.agent.KidIcarus.item.angelheart;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.AgentBody;
import kidridicarus.common.agentspine.BasicAgentSpine;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

public class AngelHeartBody extends AgentBody {
	private static final float BODY_WIDTH = UInfo.P2M(3f);
	private static final float BODY_HEIGHT = UInfo.P2M(3f);

	private BasicAgentSpine spine;

	public AngelHeartBody(AngelHeart parent, World world, Vector2 position) {
		super(parent, world);
		defineBody(new Rectangle(position.x, position.y, 0f, 0f));
	}

	@Override
	protected void defineBody(Rectangle bounds) {
		// dispose the old body if it exists	
		if(b2body != null)	
			world.destroyBody(b2body);

		setBodySize(BODY_WIDTH, BODY_HEIGHT);
		b2body = B2DFactory.makeDynamicBody(world, bounds.getCenter(new Vector2()));
		b2body.setGravityScale(0f);
		spine = new BasicAgentSpine(this);
		B2DFactory.makeSensorBoxFixture(b2body, spine.createAgentSensor(),
				CommonCF.SOLID_POWERUP_CFCAT, CommonCF.SOLID_POWERUP_CFMASK, BODY_WIDTH, BODY_HEIGHT);
	}

	public BasicAgentSpine getSpine() {
		return spine;
	}
}