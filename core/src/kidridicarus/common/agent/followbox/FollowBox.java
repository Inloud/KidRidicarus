package kidridicarus.common.agent.followbox;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import kidridicarus.agency.Agency;
import kidridicarus.agency.agent.Agent;
import kidridicarus.agency.agent.DisposableAgent;
import kidridicarus.agency.tool.ObjectProperties;

public abstract class FollowBox extends Agent implements DisposableAgent {
	protected abstract FollowBoxBody getFollowBoxBody();

	public FollowBox(Agency agency, ObjectProperties properties) {
		super(agency, properties);
	}

	/*
	 * Set the target center position of the follow box, and the box will move on update (mouse joint).
	 */
	public void setTarget(Vector2 position) {
		getFollowBoxBody().setPosition(position);
	}

	@Override
	public Vector2 getPosition() {
		return getFollowBoxBody().getPosition();
	}

	@Override
	public Rectangle getBounds() {
		return getFollowBoxBody().getBounds();
	}

	@Override
	public void disposeAgent() {
		getFollowBoxBody().dispose();
	}
}