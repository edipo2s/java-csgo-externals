package me.lixko.csgoexternals.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.github.jonatino.misc.MemoryBuffer;

import me.lixko.csgoexternals.Client;
import me.lixko.csgoexternals.Engine;
import me.lixko.csgoexternals.offsets.AttributableItemType;
import me.lixko.csgoexternals.offsets.ItemDefinitionIndex;
import me.lixko.csgoexternals.offsets.Offsets;
import me.lixko.csgoexternals.structs.CSPlayerResource;
import me.lixko.csgoexternals.util.DrawUtils;
import me.lixko.csgoexternals.util.StringFormat;
import me.lixko.csgoexternals.util.TextAlign;

public class RankReveal extends Module {

	AutoDefuse autodefusemod;

	boolean needsDataUpdate = false;
	CSPlayerResource res = new CSPlayerResource();
	MemoryBuffer resbuf = new MemoryBuffer(res.size());
	CSPlayerResource ressum = new CSPlayerResource();
	MemoryBuffer ressumbuf = new MemoryBuffer(ressum.size());
	private Map<Integer, Integer> tbs_scoreboardPlayersT = new HashMap<Integer, Integer>(); // to // sorted
	private Map<Integer, Integer> tbs_scoreboardPlayersCT = new HashMap<Integer, Integer>(); // to
	private Map<Integer, Integer> scoreboardPlayersT = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> scoreboardPlayersCT = new HashMap<Integer, Integer>();
	private int lpteamnum;
	boolean shouldDraw = false;
	boolean isCompetitive = false;
	public static final String[] csgoranks = new String[] { "unranked", "silver-1", "silver-2", "silver-3", "silver-4", "silver-5", "sem", "gold-1", "gold-2", "gold-3", "gold-master", "master-guardian-1", "master-guardian-2", "mge", "dmg", "legendary-eagle", "lem", "smfc", "global" };

	private final int SCOREBOARD_PLAYER_HEIGHT = 29;

	Thread updateLoop = new Thread(new Runnable() {
		@Override
		public void run() {
			while (Client.theClient.isRunning) {
				try {
					Thread.sleep(10);
					shouldDraw = Engine.clientModule().readInt(Offsets.input.score) > 4;
					if (!needsDataUpdate || Offsets.m_dwPlayerResourcesPointer == 0 || Offsets.m_dwPlayerResources == 0 || Offsets.m_dwLocalPlayer == 0)
						continue;

					Engine.clientModule().read(Offsets.m_dwPlayerResources, resbuf.size(), resbuf);

					byte[] m_bConnected = res.m_bConnected.getByteArray();
					int[] m_iTeam = res.m_iTeam.getIntArray();
					int[] m_iScore = res.m_iScore.getIntArray();
					int[] m_iCompetitiveRanking = res.m_iCompetitiveRanking.getIntArray();

					tbs_scoreboardPlayersT.clear();
					tbs_scoreboardPlayersCT.clear();
					isCompetitive = false;
					for (int i = 0; i < 63; i++) {
						if (m_bConnected[i] == 0)
							continue;
						if (m_iTeam[i] == 2)
							tbs_scoreboardPlayersT.put(i, m_iScore[i]);
						if (m_iTeam[i] == 3)
							tbs_scoreboardPlayersCT.put(i, m_iScore[i]);
						if (m_iCompetitiveRanking[i] > 0)
							isCompetitive = true;
					}

					scoreboardPlayersT = StringFormat.sortByValueReverse(tbs_scoreboardPlayersT, true);
					scoreboardPlayersCT = StringFormat.sortByValueReverse(tbs_scoreboardPlayersCT, true);

					lpteamnum = Engine.clientModule().readInt(Offsets.m_dwLocalPlayer + Offsets.m_iTeamNum);

					needsDataUpdate = false;
				} catch (Exception e) {
					e.printStackTrace();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
				}
			}
		}
	});

	@Override
	public void onUIRender() {
		if (!Client.theClient.isRunning || this.needsDataUpdate || !shouldDraw)
			return;

		DrawUtils.fontRenderer = DrawUtils.theme.fontRendererLarge;
		DrawUtils.enableStringBackground();
		DrawUtils.setTextColor(0x00FFFFFF);
		DrawUtils.setAlign(TextAlign.LEFT);

		int ctyoffset = 145;
		int tyoffset = 603;
		if (Math.max(scoreboardPlayersCT.size(), scoreboardPlayersT.size()) < 6) {
			ctyoffset = 377;
			tyoffset = 633;
		} else if (Math.max(scoreboardPlayersCT.size(), scoreboardPlayersT.size()) < 9) {
			ctyoffset = 288;
			tyoffset = 633;
		}

		int iter = 0;
		drawHeader(DrawUtils.getScreenHeight() - ctyoffset + SCOREBOARD_PLAYER_HEIGHT);

		for (Map.Entry<Integer, Integer> entry : scoreboardPlayersCT.entrySet()) {
			drawEntity(entry.getKey(), DrawUtils.getScreenHeight() - ctyoffset - iter * SCOREBOARD_PLAYER_HEIGHT, 3);
			iter++;
		}

		drawSum(DrawUtils.getScreenHeight() - ctyoffset - iter * SCOREBOARD_PLAYER_HEIGHT, 3);
		iter = 0;
		drawHeader(DrawUtils.getScreenHeight() - tyoffset + SCOREBOARD_PLAYER_HEIGHT);

		for (Map.Entry<Integer, Integer> entry : scoreboardPlayersT.entrySet()) {
			int ypos = DrawUtils.getScreenHeight() - tyoffset - iter * SCOREBOARD_PLAYER_HEIGHT;
			drawEntity(entry.getKey(), ypos, 2);
			iter++;
		}

		drawSum(DrawUtils.getScreenHeight() - tyoffset - iter * SCOREBOARD_PLAYER_HEIGHT, 2);

		ressumbuf.clear(ressumbuf.size());

		DrawUtils.fontRenderer = DrawUtils.theme.fontRenderer;
		this.needsDataUpdate = true;

	}

	@Override
	public void onEngineLoaded() {
		res.setSource(resbuf);
		ressum.setSource(ressumbuf);
		updateLoop.start();
		autodefusemod = (AutoDefuse) Client.theClient.moduleManager.getModule("AutoDefuse");
	}

	private void drawEntity(int resid, int y, int team) {
		DrawUtils.setAlign(TextAlign.RIGHT);
		DrawUtils.fontRenderer = DrawUtils.theme.fontRendererLarge;

		long entityptr = Engine.clientModule().readLong(Offsets.m_dwEntityList + resid * Offsets.m_dwEntityLoopDistance);
		int enthealth = res.m_iHealth.getInt(resid * Integer.BYTES);
		int entarmor = res.m_iArmor.getInt(resid * Integer.BYTES);
		ressum.m_iHealth.set(team * Integer.BYTES, ressum.m_iHealth.getInt(team * Integer.BYTES) + enthealth);
		ressum.m_iArmor.set(team * Integer.BYTES, ressum.m_iArmor.getInt(team * Integer.BYTES) + entarmor);

		if (enthealth > 0) {
			DrawUtils.setTextColor(1.0f, 0.2f, 0.2f, 1.0f);
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 + 8, y, enthealth + "");

		}

		DrawUtils.fontRenderer = DrawUtils.theme.fontRenderer;

		if (entarmor > 0) {
			if (team == 2)
				DrawUtils.setTextColor(0.878f, 0.686f, 0.337f);
			else
				DrawUtils.setTextColor(0.54f, 0.72f, 1.0f);
			String str = entarmor + "";
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 40, y, str);
			if (res.m_bHasHelmet.getBoolean(resid)) {
				DrawUtils.setLineWidth(2.0f);
				DrawUtils.setColor(0.6f, 0.6f, 0.6f, 0.8f);
				DrawUtils.drawRectangleAroundString(str, DrawUtils.getScreenWidth() / 4 - 40, y);
			}
		}

		if (autodefusemod.defuser == resid) {
			if (autodefusemod.hastimetodefuse)
				DrawUtils.setColor(0.15f, 0.8f, 0.1f, 0.8f);
			else
				DrawUtils.setColor(0.8f, 0.15f, 0.1f, 0.8f);
			DrawUtils.fillRectanglew(DrawUtils.getScreenWidth() / 4 + 60, y - 5, 27, 25);
			DrawUtils.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		}

		DrawUtils.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		DrawUtils.setTextureColor(1.0f, 1.0f, 1.0f, 1.0f);

		DrawUtils.fontRenderer = DrawUtils.theme.fontRenderer;
		DrawUtils.setTextColor(0.9f, 0.9f, 0.9f);
		DrawUtils.setAlign(TextAlign.LEFT);

		int com_t = res.m_nPersonaDataPublicCommendsTeacher.getInt(resid * Integer.BYTES);
		int com_l = res.m_nPersonaDataPublicCommendsLeader.getInt(resid * Integer.BYTES);
		int com_f = res.m_nPersonaDataPublicCommendsFriendly.getInt(resid * Integer.BYTES);
		ressum.m_nPersonaDataPublicCommendsTeacher.set(team * Integer.BYTES, ressum.m_nPersonaDataPublicCommendsTeacher.getInt(team * Integer.BYTES) + com_t);
		ressum.m_nPersonaDataPublicCommendsLeader.set(team * Integer.BYTES, ressum.m_nPersonaDataPublicCommendsLeader.getInt(team * Integer.BYTES) + com_l);
		ressum.m_nPersonaDataPublicCommendsFriendly.set(team * Integer.BYTES, ressum.m_nPersonaDataPublicCommendsFriendly.getInt(team * Integer.BYTES) + com_f);

		if (isCompetitive) {
			int rank = res.m_iCompetitiveRanking.getInt(resid * Integer.BYTES);
			int wins = res.m_iCompetitiveWins.getInt(resid * Integer.BYTES);
			ressum.m_iCompetitiveRanking.set(team * Integer.BYTES, ressum.m_iCompetitiveRanking.getInt(team * Integer.BYTES) + rank);
			ressum.m_iCompetitiveWins.set(team * Integer.BYTES, ressum.m_iCompetitiveWins.getInt(team * Integer.BYTES) + wins);
			if (rank > 0 && rank <= csgoranks.length)
				DrawUtils.drawTexture(csgoranks[rank], DrawUtils.getScreenWidth() / 4 * 3 - 10, y - 6, 51, -1);
			if (wins > 1)
				DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + 43, y + 2, wins + "");
		}

		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8), y + 2, (com_t > 0 ? com_t + "" : ""));
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8) + 30, y + 2, (com_l > 0 ? com_l + "" : ""));
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8) + 60, y + 2, (com_f > 0 ? com_f + "" : ""));

		int cashspent = res.m_iCashSpentThisRound.getInt(resid * Integer.BYTES);
		ressum.m_iCashSpentThisRound.set(team * Integer.BYTES, ressum.m_iCashSpentThisRound.getInt(team * Integer.BYTES) + cashspent);
		DrawUtils.setAlign(TextAlign.RIGHT);
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 93, y + 2, "$" + cashspent);

		DrawUtils.setAlign(TextAlign.LEFT);
		if (enthealth > 0) {
			String entweapons = "";
			int c = 0;
			long weaponhandle = (Engine.clientModule().readInt(entityptr + 0x3628) & 0xFFF);
			// ArrayList<Integer> equipment = new ArrayList<Integer>();

			int activeitem = 0;
			int lastx = 180;
			int grenades = 0;
			for (int w = 0; w < 64; w++) {
				int weaponentindex = Engine.clientModule().readInt(entityptr + 0x3528 + 4 * (w - 1)) & 0xFFF;
				if (weaponentindex == 0)
					continue;
				long weaponptr = Engine.clientModule().readLong(Offsets.m_dwEntityList + weaponentindex * Offsets.m_dwEntityLoopDistance);
				if (weaponptr == 0)
					continue;

				int m_iItemDefinitionIndex = Engine.clientModule().readInt(weaponptr + 0x34c0 + 0x60 + 0x268);
				// String name = ItemDefinitionIndex.byValue(m_iItemDefinitionIndex).name().replace("WEAPON_", "");
				// entweapons += name + ", ";
				if (weaponhandle == weaponentindex)
					activeitem = m_iItemDefinitionIndex;
				int size = 25;
				int xoff = 0;
				int xspacing = 50;
				AttributableItemType itemtype = ItemDefinitionIndex.byID(m_iItemDefinitionIndex).type();
				if (ItemDefinitionIndex.WEAPON_C4.id() == m_iItemDefinitionIndex)
					continue;
				if (ItemDefinitionIndex.WEAPON_HEGRENADE.id() == m_iItemDefinitionIndex) {
					grenades |= (1 << 0);
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 0);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_FLASHBANG.id() == m_iItemDefinitionIndex) {
					grenades |= (1 << 1);
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 1);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_SMOKEGRENADE.id() == m_iItemDefinitionIndex) {
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 2);
					grenades |= (1 << 2);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_DECOY.id() == m_iItemDefinitionIndex) {
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 3);
					grenades |= (1 << 3);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_INCGRENADE.id() == m_iItemDefinitionIndex) {
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 4);
					grenades |= (1 << 4);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_MOLOTOV.id() == m_iItemDefinitionIndex) {
					if (weaponhandle == weaponentindex)
						grenades |= 1 << (8 + 5);
					grenades |= (1 << 5);
					continue;
				}
				if (ItemDefinitionIndex.WEAPON_TASER.id() == m_iItemDefinitionIndex) {
					size = 25;
					xoff = 280;
				}
				switch (itemtype) {
				case PISTOL:
					xoff = 115;
					size = 25;
					break;
				case AUTOMATIC:
					xoff = 1;
					size = 30;
					break;
				case SHOTGUN:
					size = 30;
					xoff = 1;
					break;
				case SNIPER:
					size = 30;
					xoff = 1;
					break;
				case KNIFE:
					continue;
				case OTHER:
					break;
				default:
					break;
				}

				DrawUtils.setAlign(TextAlign.RIGHT);
				if (xoff == 0) {
					lastx -= xspacing;
					c++;
					DrawUtils.setAlign(TextAlign.CENTER);
				}
				DrawUtils.setTextureColor(0.75f, 0.75f, 0.75f, 0.6f);
				if (weaponhandle == weaponentindex)
					DrawUtils.setTextureColor(1f, 1f, 1f, 0.8f);
				DrawUtils.drawTexture("weapon_" + m_iItemDefinitionIndex, xoff > 0 ? (DrawUtils.getScreenWidth() / 6 - xoff) : lastx, y - 10, -1, size);
				DrawUtils.setTextureColor(0f, 0f, 0f);
				if (weaponhandle == weaponentindex)
					DrawUtils.setTextureColor(1.0f, 0.0f, 0f);
				DrawUtils.drawTexture("weaponout_" + m_iItemDefinitionIndex, xoff > 0 ? (DrawUtils.getScreenWidth() / 6 - xoff) : lastx, y - 10, -1, size);
			}

			lastx -= 30;
			for (int i = 0; i < 6; i++) {
				int m_iItemDefinitionIndex = 0;
				if (i == 0 && (1 << 0 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_HEGRENADE.id();
				else if (i == 1 && (1 << 1 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_FLASHBANG.id();
				else if (i == 2 && (1 << 2 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_SMOKEGRENADE.id();
				else if (i == 3 && (1 << 3 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_DECOY.id();
				else if (i == 4 && (1 << 4 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_INCGRENADE.id();
				else if (i == 5 && (1 << 5 & grenades) > 0)
					m_iItemDefinitionIndex = ItemDefinitionIndex.WEAPON_MOLOTOV.id();
				else
					continue;
				boolean inuse = (1 << (i + 8) & grenades) > 0;
				DrawUtils.setAlign(TextAlign.CENTER);
				DrawUtils.setTextureColor(0.75f, 0.75f, 0.75f, 0.6f);
				if (inuse)
					DrawUtils.setTextureColor(1f, 1f, 1f, 0.8f);
				DrawUtils.drawTexture("weapon_" + m_iItemDefinitionIndex, lastx, y - 10, -1, 25);
				DrawUtils.setTextureColor(0f, 0f, 0f);
				if (inuse)
					DrawUtils.setTextureColor(1.0f, 0.0f, 0f);
				DrawUtils.drawTexture("weaponout_" + m_iItemDefinitionIndex, lastx, y - 10, -1, 25);
				lastx -= 30;
			}
			DrawUtils.setAlign(TextAlign.LEFT);
			if (res.m_iPlayerC4.getInt() == resid && (lpteamnum != 2 || activeitem == ItemDefinitionIndex.WEAPON_C4.id())) {
				DrawUtils.setColor(1.0f, 1.0f, 1.0f, 1f);
				DrawUtils.fillRectanglew(DrawUtils.getScreenWidth() / 4 + 60, y - 8, 28, 26);
				DrawUtils.drawTexture("bomb", DrawUtils.getScreenWidth() / 4 + 60, y - 6, 25, -1);
			}

			DrawUtils.setTextureColor(1f, 1f, 1f);
			if (res.m_bHasDefuser.getBoolean(resid) && (lpteamnum != 3 || autodefusemod.defuser == resid))
				DrawUtils.drawTexture("defuser", DrawUtils.getScreenWidth() / 4 + 63, y - 4, 22, -1);

			if (res.m_bHasDefuser.getBoolean(resid) && (lpteamnum != 3 || autodefusemod.defuser == resid))
				DrawUtils.drawTexture("defuser", DrawUtils.getScreenWidth() / 4 + 63, y - 4, 22, -1);
		}
		DrawUtils.setTextureColor(1f, 1f, 1f);
		ressum.m_iKills.set(team * Integer.BYTES, res.m_iKills.getInt(resid * Integer.BYTES) + ressum.m_iKills.getInt(team * Integer.BYTES));
		ressum.m_iAssists.set(team * Integer.BYTES, res.m_iAssists.getInt(resid * Integer.BYTES) + ressum.m_iAssists.getInt(team * Integer.BYTES));
		ressum.m_iDeaths.set(team * Integer.BYTES, res.m_iDeaths.getInt(resid * Integer.BYTES) + ressum.m_iDeaths.getInt(team * Integer.BYTES));
		ressum.m_iScore.set(team * Integer.BYTES, res.m_iScore.getInt(resid * Integer.BYTES) + ressum.m_iScore.getInt(team * Integer.BYTES));
		ressum.m_iPing.set(team * Integer.BYTES, res.m_iPing.getInt(resid * Integer.BYTES) + ressum.m_iPing.getInt(team * Integer.BYTES));
	}

	private void drawHeader(int y) {
		DrawUtils.fontRenderer = DrawUtils.theme.fontRenderer;
		DrawUtils.setTextColor(1.0f, 0.4f, 0.4f);
		DrawUtils.setAlign(TextAlign.RIGHT);
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 + 8, y, "+");
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 40, y, "Arm");

		DrawUtils.setTextColor(0.6f, 0.6f, 0.6f);
		DrawUtils.setColor(0f, 0f, 0f, 0.1f);
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 93, y, "Spent");

		DrawUtils.setAlign(TextAlign.LEFT);
		if (isCompetitive)
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 - 8, y, "Rank  Wins");

		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8), y, "Tea Lea Fri");
	}

	private void drawSum(int y, int team) {

		DrawUtils.setAlign(TextAlign.RIGHT);
		DrawUtils.fontRenderer = DrawUtils.theme.fontRenderer;
		DrawUtils.setColor(0.1f, 0.1f, 0.1f, 0.92f);
		if (team == 2)
			DrawUtils.setColor(0.878f, 0.686f, 0.337f, 0.7f);
		else
			DrawUtils.setColor(0.54f, 0.72f, 1.0f, 0.7f);
		DrawUtils.fillRectangle(DrawUtils.getScreenWidth() / 6 + 10, y + 20, DrawUtils.getScreenWidth() / 5 * 4 - 23 + (isCompetitive ? 100 : 0), y + 20 + 2);
		DrawUtils.setColor(1f, 1f, 1f);

		if (ressum.m_iHealth.getInt(team * Integer.BYTES) > 0) {
			DrawUtils.setTextColor(1.0f, 0.2f, 0.2f, 1.0f);
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 + 8, y, ressum.m_iHealth.getInt(team * Integer.BYTES) + "");
		}

		if (team == 2)
			DrawUtils.setTextColor(0.878f, 0.686f, 0.337f);
		else
			DrawUtils.setTextColor(0.54f, 0.72f, 1.0f);

		if (ressum.m_iArmor.getInt(team * Integer.BYTES) > 0) {
			String str = ressum.m_iArmor.getInt(team * Integer.BYTES) + "";
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 40, y, str);
		}

		DrawUtils.drawString(DrawUtils.getScreenWidth() / 6 * 4 - 70, y, ressum.m_iKills.getInt(team * Integer.BYTES) + "");
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 6 * 4 - 22, y, ressum.m_iAssists.getInt(team * Integer.BYTES) + "");
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 6 * 4 + 26, y, ressum.m_iDeaths.getInt(team * Integer.BYTES) + "");
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 6 * 4 + 138, y, ressum.m_iScore.getInt(team * Integer.BYTES) + "");
		if (ressum.m_iPing.getInt(team * Integer.BYTES) > 0)
			DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 + 56, y, ressum.m_iPing.getInt(team * Integer.BYTES) + "");
		DrawUtils.setAlign(TextAlign.LEFT);

		int com_t = ressum.m_nPersonaDataPublicCommendsTeacher.getInt(team * Integer.BYTES);
		int com_l = ressum.m_nPersonaDataPublicCommendsLeader.getInt(team * Integer.BYTES);
		int com_f = ressum.m_nPersonaDataPublicCommendsFriendly.getInt(team * Integer.BYTES);

		if (isCompetitive) {
			int rank = ressum.m_iCompetitiveRanking.getInt(team * Integer.BYTES);
			int wins = ressum.m_iCompetitiveWins.getInt(team * Integer.BYTES);
			if (rank > 0)
				DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 - 10, y + 2, rank + "");
			if (wins > 1)
				DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + 43, y + 2, wins + "");
		}

		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8), y, (com_t > 0 ? com_t + "" : ""));
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8) + 30, y, (com_l > 0 ? com_l + "" : ""));
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 * 3 + (isCompetitive ? 90 : -8) + 60, y, (com_f > 0 ? com_f + "" : ""));

		int cashspent = ressum.m_iCashSpentThisRound.getInt(team * Integer.BYTES);
		DrawUtils.setAlign(TextAlign.RIGHT);
		DrawUtils.drawString(DrawUtils.getScreenWidth() / 4 - 93, y + 2, "$" + cashspent);
	}

}
