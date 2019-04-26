package kidridicarus.game.SMB1.agent;

import kidridicarus.agency.agent.Agent;
import kidridicarus.game.SMB1.agent.other.bumptile.BumpTile.TileBumpStrength;

// a tile agent that can be bumped (i.e. take bumps)
public interface TileBumpTakeAgent {
	// tile bumped from below when player jump punched the tile
	public boolean onTakeTileBump(Agent agent, TileBumpStrength strength);
}