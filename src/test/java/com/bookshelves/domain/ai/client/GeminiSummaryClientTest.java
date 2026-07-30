package com.bookshelves.domain.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class GeminiSummaryClientTest {

  private MockRestServiceServer mockServer;
  private GeminiSummaryClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    mockServer = MockRestServiceServer.bindTo(builder).build();
    client =
        new GeminiSummaryClient(
            builder.baseUrl(GeminiQuestionClient.BASE_URL).build(),
            new ObjectMapper(),
            "test-api-key",
            GeminiQuestionClient.DEFAULT_MODEL);
  }

  private Book book() {
    return Book.builder().isbn("9788936434595").title("아몬드").author("손원평").build();
  }

  private AIQuestion question(SeedQuestion seed) {
    return AIQuestion.builder()
        .content(seed.getContent())
        .questionOrder(seed.getQuestionOrder())
        .build();
  }

  private ChatMessage message(Long memberId, String text) {
    Member member = Member.createSocialMember(null, "provider-id-" + memberId);
    ReflectionTestUtils.setField(member, "id", memberId);
    ReflectionTestUtils.setField(member, "nickname", "책 먹는 여우");
    return ChatMessage.builder()
        .chatRoom(ChatRoom.create(Meeting.builder().build()))
        .senderMember(member)
        .message(text)
        .build();
  }

  private void respondWith(String modelText) {
    mockServer
        .expect(
            requestTo(
                GeminiQuestionClient.BASE_URL
                    + "/models/"
                    + GeminiQuestionClient.DEFAULT_MODEL
                    + ":generateContent"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}]}
                """
                    .formatted(new ObjectMapper().valueToTree(modelText).toString()),
                MediaType.APPLICATION_JSON));
  }

  @Test
  void returnsDraftsByAxis() {
    respondWith(
        """
        [{"axis":"KEY_ARGUMENT","title":"감정을 배울 수 있는가","summary":"참여자들은 윤재의 변화를 두고 의견이 갈렸다."},
         {"axis":"LIFE_LINK","title":"현재의 나와 연결되는 부분","summary":"자신의 무감함을 떠올린 참여자가 있었다."}]
        """);

    Map<SummaryAxis, GeminiSummaryClient.SummaryDraft> drafts =
        client.generateSummaries(
            book(),
            List.of(question(SeedQuestion.READING_IMPRESSION)),
            List.of(message(1L, "윤재가 변한 게 맞나요")));

    assertThat(drafts).containsOnlyKeys(SummaryAxis.KEY_ARGUMENT, SummaryAxis.LIFE_LINK);
    assertThat(drafts.get(SummaryAxis.KEY_ARGUMENT).title()).isEqualTo("감정을 배울 수 있는가");
  }

  @Test
  void dropsUnknownAxisAndDuplicateAxis() {
    respondWith(
        """
        [{"axis":"NOT_AN_AXIS","title":"버려진다","summary":"..."},
         {"axis":"REACTION","title":"먼저 온 것이 채택된다","summary":"..."},
         {"axis":"REACTION","title":"나중 것은 무시된다","summary":"..."}]
        """);

    Map<SummaryAxis, GeminiSummaryClient.SummaryDraft> drafts =
        client.generateSummaries(book(), List.of(), List.of(message(1L, "안녕하세요")));

    assertThat(drafts).containsOnlyKeys(SummaryAxis.REACTION);
    assertThat(drafts.get(SummaryAxis.REACTION).title()).isEqualTo("먼저 온 것이 채택된다");
  }

  @Test
  void dropsBlankTitleAndOverlongTitle() {
    respondWith(
        """
        [{"axis":"KEY_ARGUMENT","title":"","summary":"제목이 비어 버려진다"},
         {"axis":"REACTION","title":"%s","summary":"제목이 길어 버려진다"},
         {"axis":"LIFE_LINK","title":"살아남는 제목","summary":"본문"}]
        """
            .formatted("가".repeat(61)));

    Map<SummaryAxis, GeminiSummaryClient.SummaryDraft> drafts =
        client.generateSummaries(book(), List.of(), List.of(message(1L, "안녕하세요")));

    assertThat(drafts).containsOnlyKeys(SummaryAxis.LIFE_LINK);
  }

  @Test
  void promptReplacesNicknameWithAnonymousLabel() {
    String prompt =
        client.buildPrompt(
            book(),
            List.of(question(SeedQuestion.RATING)),
            List.of(message(1L, "첫 번째 사람"), message(2L, "두 번째 사람"), message(1L, "다시 첫 사람")));

    assertThat(prompt).doesNotContain("책 먹는 여우");
    assertThat(prompt).contains("참여자 A").contains("참여자 B");
    // 같은 회원은 모임 안에서 같은 라벨을 유지해야 의견 대립을 추적할 수 있다
    assertThat(prompt.indexOf("참여자 A: 첫 번째 사람")).isGreaterThan(0);
    assertThat(prompt.indexOf("참여자 A: 다시 첫 사람")).isGreaterThan(0);
  }
}
