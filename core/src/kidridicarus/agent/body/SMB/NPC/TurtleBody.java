package kidridicarus.agent.body.SMB.NPC;

import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import kidridicarus.agency.contact.AgentBodyFilter;
import kidridicarus.agency.contact.CFBitSeq;
import kidridicarus.agency.contact.CFBitSeq.CFBit;
import kidridicarus.agent.Agent;
import kidridicarus.agent.SMB.NPC.Turtle;
import kidridicarus.agent.body.MobileAgentBody;
import kidridicarus.agent.body.optional.BumpableBody;
import kidridicarus.agent.body.sensor.AgentContactBeginSensor;
import kidridicarus.agent.body.sensor.AgentContactSensor;
import kidridicarus.agent.body.sensor.OnGroundSensor;
import kidridicarus.agent.body.sensor.SolidBoundSensor;
import kidridicarus.info.UInfo;
import kidridicarus.tool.B2DFactory;

public class TurtleBody extends MobileAgentBody implements BumpableBody {
	private static final float BODY_WIDTH = UInfo.P2M(14f);
	private static final float BODY_HEIGHT = UInfo.P2M(14f);
	private static final float FOOT_WIDTH = UInfo.P2M(12f);
	private static final float FOOT_HEIGHT = UInfo.P2M(4f);

	private Turtle parent;
	private OnGroundSensor ogSensor;
	private SolidBoundSensor hmSensor;
	private AgentContactSensor acSensor;
	private AgentContactBeginSensor kickSensor;

	public TurtleBody(Turtle parent, World world, Vector2 position) {
		this.parent = parent;
		defineBody(world, position);
	}

	private void defineBody(World world, Vector2 position) {
		setBodySize(BODY_WIDTH, BODY_HEIGHT);
		createSolidBody(world, position);
		createAgentSensor();
		createGroundSensor();
	}

	private void createSolidBody(World world, Vector2 position) {
		CFBitSeq catBits = new CFBitSeq(CFBit.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.SOLID_BOUND_BIT);
		hmSensor = new SolidBoundSensor(parent);
		b2body = B2DFactory.makeBoxBody(world, BodyType.DynamicBody, hmSensor, catBits, maskBits, position,
				BODY_WIDTH, BODY_HEIGHT);
	}

	private void createAgentSensor() {
		FixtureDef fdef = new FixtureDef();
		PolygonShape boxShape = new PolygonShape();
		boxShape.setAsBox(BODY_WIDTH/2f, BODY_HEIGHT/2f);
		fdef.isSensor = true;
		fdef.shape = boxShape;
		CFBitSeq catBits = new CFBitSeq(CFBit.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.AGENT_BIT);
		acSensor = new AgentContactSensor(this);
		kickSensor = new AgentContactBeginSensor(this);
		kickSensor.chainTo(acSensor);
		b2body.createFixture(fdef).setUserData(new AgentBodyFilter(catBits, maskBits, kickSensor));
	}

	private void createGroundSensor() {
		FixtureDef fdef = new FixtureDef();
		PolygonShape boxShape = new PolygonShape();
		boxShape.setAsBox(FOOT_WIDTH/2f, FOOT_HEIGHT/2f, new Vector2(0f, -BODY_HEIGHT/2f), 0f);
		fdef.isSensor = true;
		fdef.shape = boxShape;
		CFBitSeq catBits = new CFBitSeq(CFBit.AGENT_BIT);
		CFBitSeq maskBits = new CFBitSeq(CFBit.SOLID_BOUND_BIT);
		ogSensor = new OnGroundSensor(null);
		b2body.createFixture(fdef).setUserData(new AgentBodyFilter(catBits, maskBits, ogSensor));
	}

	@Override
	public void onBump(Agent bumpingAgent) {
		parent.onBump(bumpingAgent);
	}

	public boolean isMoveBlocked(boolean moveRight) {
		return hmSensor.isHMoveBlocked(getBounds(), moveRight);
	}

	public boolean isMoveBlockedByAgent(boolean moveRight) {
		return AgentContactSensor.isMoveBlockedByAgent(acSensor, getPosition(), moveRight);
	}

	public boolean isOnGround() {
		// return true if the on ground contacts list contains at least 1 floor
		return ogSensor.isOnGround();
	}

	public List<Agent> getAndResetContactBeginAgents() {
		return kickSensor.getAndResetContacts();
	}

	@Override
	public Agent getParent() {
		return parent;
	}
}
