package jobs;

import java.util.ArrayList;
import java.util.List;

import models.Extra;
import models.ExtraTip;
import models.Game;
import models.GameTip;
import models.User;
import play.Logger;
import play.jobs.On;
import services.MailService;
import utils.AppUtils;

@On("0 0 1 * * ?")
public class ReminderJob extends AppJob {

	public ReminderJob() {
		this.setDescription("Sends a reminder email to every activated user, reminding them of tips for games and extra.");
		this.setExecuted("Runs daily at 01:00");
	}

	@Override
	public void doJob() {
		if (AppUtils.isJobInstance()) {
			Logger.info("Running job: ReminderJob");
			final List<Extra> nextExtras = Extra.find("SELECT e FROM Extra e WHERE DATE(ending) = DATE(NOW())").fetch();
			final List<Game> nextGames = Game.find("SELECT g FROM Game g WHERE DATE(kickoff) = DATE(NOW())").fetch();
			final List<User> users = User.find("byReminderAndActive", true, true).fetch();

			for (final User user : users) {
				final List<Game> reminderGames = new ArrayList();
				final List<Extra> reminderBonus = new ArrayList();

				for (final Game game : nextGames) {
					final GameTip gameTip = GameTip.find("byGameAndUser", game, user).first();
					if (gameTip == null) {
						reminderGames.add(game);
					}
				}

				for (final Extra extra : nextExtras) {
					final ExtraTip extraTip = ExtraTip.find("byExtraAndUser", extra, user).first();
					if (extraTip == null) {
						reminderBonus.add(extra);
					}
				}

				if ((reminderGames.size() > 0) || (reminderBonus.size() > 0)) {
					MailService.reminder(user, reminderGames, reminderBonus);
					Logger.info("Reminder send to: " + user.getUsername());
				}
			}
		}
	}
}