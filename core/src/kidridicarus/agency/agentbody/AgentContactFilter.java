package kidridicarus.agency.agentbody;

import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.Fixture;

/*
 * Implement an infinite bit contact filter scheme by way of the enum ContactBit and the class CustomFilter.
 */
public class AgentContactFilter implements ContactFilter {
	@Override
	public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) {
		if(!(fixtureA.getUserData() instanceof AgentBodyFilter) ||
				!(fixtureB.getUserData() instanceof AgentBodyFilter)) {
			return false;
		}
		return AgentBodyFilter.isContact((AgentBodyFilter) fixtureA.getUserData(),
				(AgentBodyFilter) fixtureB.getUserData());
	}
}
