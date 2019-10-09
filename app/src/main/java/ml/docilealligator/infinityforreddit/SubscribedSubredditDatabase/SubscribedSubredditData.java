package ml.docilealligator.infinityforreddit.SubscribedSubredditDatabase;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import ml.docilealligator.infinityforreddit.Account.Account;

@Entity(tableName = "subscribed_subreddits", foreignKeys = @ForeignKey(entity = Account.class, parentColumns = "username",
        childColumns = "username", onDelete = ForeignKey.CASCADE))
public class SubscribedSubredditData {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "icon")
    private String iconUrl;
    @ColumnInfo(name = "username")
    private String username;
    @ColumnInfo(name = "is_favorite")
    private boolean favorite;

    public SubscribedSubredditData(@NonNull String id, String name, String iconUrl, String username,
                                   boolean favorite) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.username = username;
        this.favorite = favorite;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
