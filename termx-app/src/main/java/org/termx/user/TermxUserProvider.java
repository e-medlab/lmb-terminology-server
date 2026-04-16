package org.termx.user;

import com.kodality.commons.client.HttpClient;
import com.kodality.commons.util.JsonUtil;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.termx.core.user.User;
import org.termx.core.user.UserProvider;

@Requires(property = "nsoft.url")
@Slf4j
@Singleton
public class TermxUserProvider extends UserProvider {
  private final HttpClient client;
  private final String nsoftUrl;

  public TermxUserProvider(@Value("${nsoft.url}") String nsoftUrl) {
    this.client = new HttpClient(nsoftUrl);
    this.nsoftUrl = nsoftUrl;
  }

  @Override
  public List<User> getUsers() {
    String path = "/info/users";
    long startedAt = System.currentTimeMillis();
    log.info("Loading users from nsoft endpoint '{}{}'", nsoftUrl, path);
    try {
      HttpResponse<String> response = client.GET(path).join();
      String body = response.body();
      log.info("nsoft user response status={}, durationMs={}, bodyLength={}",
          response.statusCode(), System.currentTimeMillis() - startedAt, body == null ? 0 : body.length());
      if (log.isDebugEnabled()) {
        log.debug("nsoft user response body preview={}", StringUtils.abbreviate(body, 500));
      }

      List<NsoftUser> nsoftUsers = JsonUtil.fromJson(body, JsonUtil.getListType(NsoftUser.class));
      long missingEmailCount = nsoftUsers.stream().map(NsoftUser::getEmail).filter(StringUtils::isBlank).count();
      long missingPermissionsCount = nsoftUsers.stream().map(NsoftUser::getPermissions).filter(CollectionUtils::isEmpty).count();
      log.info("Parsed {} users from nsoft, missingEmail={}, missingPermissions={}",
          nsoftUsers.size(), missingEmailCount, missingPermissionsCount);
      if (nsoftUsers.isEmpty()) {
        log.warn("nsoft returned an empty user list from '{}{}'", nsoftUrl, path);
      }

      return nsoftUsers.stream()
          .map(u -> new User().setSub(u.getEmail()).setName(getName(u)).setPrivileges(getPrivileges(u)))
          .collect(Collectors.toList());
    } catch (RuntimeException e) {
      log.error("Failed to load users from nsoft endpoint '{}{}': {}", nsoftUrl, path, e.getMessage(), e);
      throw e;
    }
  }

  private String getName(NsoftUser user) {
    if (user.getFirstname() != null && user.getLastname() != null) {
      return String.join(",", user.getLastname(), user.getFirstname());
    }
    return user.getEmail();
  }

  private Set<String> getPrivileges(NsoftUser user) {
    if (user.getPermissions() == null) {
      return Set.of();
    }
    return new HashSet<>(user.getPermissions());
  }

  @Getter
  @Setter
  private static class NsoftUser {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private List<String> permissions;
  }
}
