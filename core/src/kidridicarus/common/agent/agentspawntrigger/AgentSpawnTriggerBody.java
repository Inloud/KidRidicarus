package kidridicarus.common.agent.agentspawntrigger;

import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;

import kidridicarus.agency.agentbody.AgentBody;
import kidridicarus.agency.agentcontact.CFBitSeq;
import kidridicarus.common.agent.optional.TriggerableAgent;
import kidridicarus.common.agentsensor.AgentContactHoldSensor;
import kidridicarus.common.info.CommonCF;
import kidridicarus.common.info.UInfo;
import kidridicarus.common.tool.B2DFactory;

public class AgentSpawnTriggerBody extends AgentBody {
	private static final CFBitSeq CFCAT_BITS = new CFBitSeq(CommonCF.Alias.SPAWNTRIGGER_BIT);
	private static final CFBitSeq CFMASK_BITS = new CFBitSeq(CommonCF.Alias.SPAWNBOX_BIT);
	// if the target position is at least this far away from the current position then reset the b2body
	// TODO: is 50 pixels right?
	private static final float RESET_DIST = UInfo.P2M(50);

	private AgentSpawnTrigger parent;
	private MouseJoint mj;
	private World world;
	private AgentContactHoldSensor acSensor;

	public AgentSpawnTriggerBody(AgentSpawnTrigger parent, World world, Rectangle bounds) {
		this.parent = parent;
		this.world = world;
		defineBody(world, bounds);
	}

	private void defineBody(World world, Rectangle bounds) {
		// destroy the old bodies if necessary
		if(mj != null && mj.getBodyA() != null)
			world.destroyBody(mj.getBodyA());	// destroy the temp bodyA used by mouse joint
		if(b2body != null)
			world.destroyBody(b2body);

		setBodySize(bounds.width, bounds.height);
		createRegBody(world, bounds);
		createMouseJoint(world, bounds.getCenter(new Vector2()));
	}

	private void createRegBody(World world, Rectangle bounds) {
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.DynamicBody;
		bdef.position.set(bounds.getCenter(new Vector2()));
		bdef.gravityScale = 0f;
		b2body = world.createBody(bdef);

		FixtureDef fdef = new FixtureDef();
		fdef.isSensor = true;
		acSensor = new AgentContactHoldSensor(this);
		B2DFactory.makeBoxFixture(b2body, fdef, acSensor, CFCAT_BITS, CFMASK_BITS,
				bounds.width, bounds.height);
	}

	public List<TriggerableAgent> getSpawnerContacts() {
		return acSensor.getContactsByClass(TriggerableAgent.class);
	}

	// mouse joint allows us to quickly change the position of the spawn trigger body without breaking Box2D
	private void createMouseJoint(World world, Vector2 position) {
		Body tempB;	// mouse joint body

		// mouse joint needs a fake body, so create a sensor body with no gravity
		BodyDef bdef = new BodyDef();
		bdef.type = BodyDef.BodyType.DynamicBody;
		bdef.position.set(0f, 0f);
		bdef.gravityScale = 0f;
		tempB = world.createBody(bdef);

		FixtureDef fdef = new FixtureDef();
		fdef.isSensor = true;
		// the fake body does not contact anything
		// TODO: find a better place to stick this temp body 
		B2DFactory.makeBoxFixture(tempB, fdef, this, CommonCF.NO_CONTACT_CFCAT,
				CommonCF.NO_CONTACT_CFMASK, 0.01f, 0.01f);

		MouseJointDef mjdef = new MouseJointDef();
		// this body is supposedly ignored by box2d, but needs to be a valid non-static body (non-sensor also?)
		mjdef.bodyA = tempB;
		// this is the body that will move to "catch up" to the mouse joint target
		mjdef.bodyB = b2body;
		mjdef.maxForce = 5000f * b2body.getMass();
		mjdef.frequencyHz = 5f;
		mjdef.dampingRatio = 0.9f;
		mjdef.target.set(position);
		mj = (MouseJoint) world.createJoint(mjdef);
		mj.setTarget(position);
	}

	public void setPosition(Vector2 position) {
		Vector2 diff = position.cpy().sub(b2body.getPosition());
		if(diff.len() > RESET_DIST)
			resetPosition(position);
		else
			mj.setTarget(position);
	}

	private void resetPosition(Vector2 position) {
		Rectangle oldBounds = getBounds();
		defineBody(b2body.getWorld(), new Rectangle(position.x - oldBounds.width/2f,
				position.y - oldBounds.height/2f, oldBounds.width, oldBounds.height));
	}

	@Override
	public AgentSpawnTrigger getParent() {
		return parent;
	}

	@Override
	public void dispose() {
		world.destroyBody(mj.getBodyA());	// destroy the temp bodyA used by mouse joint
		world.destroyBody(b2body);
	}
}