package kidridicarus.game.info;

import kidridicarus.agency.AgentClassList;
import kidridicarus.game.SMB.agent.NPC.Goomba;
import kidridicarus.game.SMB.agent.NPC.Turtle;
import kidridicarus.game.SMB.agent.item.FireFlower;
import kidridicarus.game.SMB.agent.item.Mush1UP;
import kidridicarus.game.SMB.agent.item.PowerMushroom;
import kidridicarus.game.SMB.agent.item.PowerStar;
import kidridicarus.game.SMB.agent.item.StaticCoin;
import kidridicarus.game.SMB.agent.other.BrickPiece;
import kidridicarus.game.SMB.agent.other.CastleFlag;
import kidridicarus.game.SMB.agent.other.Flagpole;
import kidridicarus.game.SMB.agent.other.FloatingPoints;
import kidridicarus.game.SMB.agent.other.SpinCoin;
import kidridicarus.game.SMB.agent.player.Luigi;
import kidridicarus.game.SMB.agent.player.Mario;
import kidridicarus.game.SMB.agent.player.MarioFireball;
import kidridicarus.game.agent.SMB.other.bumptile.BumpTile;

/*
 * Title: Super Mario Bros. Info
 * Desc: Info specific to Super Mario Bros.
 */
public class SMBInfo {
	// https://www.mariowiki.com/Point
	//   Super Mario Bros. 1
	//     100 - 200 - 400 - 500 - 800 - 1000 - 2000 - 4000 - 5000 - 8000 - 1UP 
	public enum PointAmount {
		// Points begin at zero, and end at infinity.
		// Zero is used to mark uninitialized variables.
		// Infinity is used to mark overflow (incrementing past known values).
		ZERO(0), P100(100), P200(200), P400(400), P500(500), P800(800), P1000(1000), P2000(2000), P4000(4000),
			P5000(5000), P8000(8000), P1UP(0);
		private int amt;
		PointAmount(int amt) { this.amt = amt; }
		public int getIntAmt() { return amt; }
		public PointAmount increment() {
			if(ordinal()+1 < values().length)
				return PointAmount.values()[ordinal()+1];
			// when points overflow occurs, the player gets 1-UP
			else
				return PointAmount.P1UP;
		}
	}

	public static final AgentClassList SMB_AGENT_CLASSLIST = new AgentClassList( 
			GameKV.SMB.AgentClassAlias.VAL_BRICKPIECE, BrickPiece.class,
			GameKV.SMB.AgentClassAlias.VAL_BUMPTILE, BumpTile.class,
			GameKV.SMB.AgentClassAlias.VAL_CASTLEFLAG, CastleFlag.class,
			GameKV.SMB.AgentClassAlias.VAL_COIN, StaticCoin.class,
			GameKV.SMB.AgentClassAlias.VAL_FIREFLOWER, FireFlower.class,
			GameKV.SMB.AgentClassAlias.VAL_FLAGPOLE, Flagpole.class,
			GameKV.SMB.AgentClassAlias.VAL_FLOATINGPOINTS, FloatingPoints.class,
			GameKV.SMB.AgentClassAlias.VAL_GOOMBA, Goomba.class,
			GameKV.SMB.AgentClassAlias.VAL_LUIGI, Luigi.class,
			GameKV.SMB.AgentClassAlias.VAL_MARIO, Mario.class,
			GameKV.SMB.AgentClassAlias.VAL_MARIOFIREBALL, MarioFireball.class,
			GameKV.SMB.AgentClassAlias.VAL_MUSHROOM, PowerMushroom.class,
			GameKV.SMB.AgentClassAlias.VAL_MUSH1UP, Mush1UP.class,
			GameKV.SMB.AgentClassAlias.VAL_POWERSTAR, PowerStar.class,
			GameKV.SMB.AgentClassAlias.VAL_SPINCOIN, SpinCoin.class,
			GameKV.SMB.AgentClassAlias.VAL_TURTLE, Turtle.class);
}
