package kidridicarus.common.agent.despawnbox;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.tool.B2DFactory;

public class DespawnBoxBody extends AgentBody {
	private static final CFBitSeq CFCAT_BITS = new CFBitSeq(CommonCF.Alias.DESPAWN_BIT);
	private static final CFBitSeq CFMASK_BITS = new CFBitSeq(CommonCF.Alias.AGENT_BIT);

	private DespawnBox parent;

	public DespawnBoxBody(DespawnBox parent, World world, Rectangle bounds) {
		this.parent = parent;
		setBodySize(bounds.width, bounds.height);
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.StaticBody;
		bdef.position.set(bounds.getCenter(new Vector2()));
		b2body = world.createBody(bdef);

		FixtureDef fdef = new FixtureDef();
		fdef.isSensor = true;
		B2DFactory.makeBoxFixture(b2body, fdef, this, CFCAT_BITS, CFMASK_BITS, bounds.width, bounds.height);
	}

	@Override
	public DespawnBox getParent() {
		return parent;
	}
}