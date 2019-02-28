package kidridicarus.agency.agent.body.general;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.agent.body.AgentBody;
import kidridicarus.agency.agent.general.AgentSpawner;
import kidridicarus.agency.contact.CFBitSeq;
import kidridicarus.agency.tool.B2DFactory;
import kidridicarus.game.info.GameInfo;

public class AgentSpawnerBody extends AgentBody {
	private AgentSpawner parent;

	public AgentSpawnerBody(AgentSpawner parent, World world, Rectangle bounds) {
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
		CFBitSeq catBits = new CFBitSeq(GameInfo.CFBits.SPAWNBOX_BIT);
		CFBitSeq maskBits = new CFBitSeq(GameInfo.CFBits.SPAWNTRIGGER_BIT);
		b2body = B2DFactory.makeSpecialBoxBody(world, bdef, fdef, this, catBits, maskBits, bounds.width, bounds.height);
	}

	@Override
	public AgentSpawner getParent() {
		return parent;
	}
}