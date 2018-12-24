package kidridicarus.agent.bodies.SMB;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.B2DFactory;
import kidridicarus.agency.contacts.CFBitSeq;
import kidridicarus.agency.contacts.CFBitSeq.CFBit;
import kidridicarus.agent.Agent;
import kidridicarus.agent.SMB.BumpTile;
import kidridicarus.agent.bodies.AgentBody;
import kidridicarus.agent.bodies.optional.BumpableTileBody;

public class BumpTileBody extends AgentBody implements BumpableTileBody {
	private BumpTile parent;

	public BumpTileBody(World world, BumpTile parent, Rectangle bounds) {
		this.parent = parent;
		setBodySize(bounds.width, bounds.height);

		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.StaticBody;
		bdef.position.set(bounds.getCenter(new Vector2()));
		FixtureDef fdef = new FixtureDef();
		fdef.isSensor = true;
		CFBitSeq catBits = new CFBitSeq(CFBit.BUMPABLE_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.GUIDE_SENSOR_BIT);
		b2body = B2DFactory.makeSpecialBoxBody(world, bdef, fdef, this, catBits, maskBits, bounds.width,
				bounds.height);
	}

	@Override
	public Agent getParent() {
		return parent;
	}

	@Override
	public void onBumpTile(Agent bumpingAgent) {
		parent.onBumpTile(bumpingAgent);
	}
}
