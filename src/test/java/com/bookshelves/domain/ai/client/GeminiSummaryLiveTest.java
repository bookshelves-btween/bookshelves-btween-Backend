package com.bookshelves.domain.ai.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.bookshelves.domain.ai.entity.AIQuestion;
import com.bookshelves.domain.ai.enums.SeedQuestion;
import com.bookshelves.domain.ai.enums.SummaryAxis;
import com.bookshelves.domain.book.entity.Book;
import com.bookshelves.domain.chat.entity.ChatMessage;
import com.bookshelves.domain.chat.entity.ChatRoom;
import com.bookshelves.domain.meeting.entity.Meeting;
import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

// 실제 Gemini API를 호출해 요약 결과를 눈으로 확인하는 수동 테스트.
// GEMINI_API_KEY가 없으면 통째로 skip되므로 CI에는 영향이 없다.
//
// 실행: GEMINI_API_KEY=... ./gradlew test --tests '*GeminiSummaryLiveTest' -i
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiSummaryLiveTest {

  @BeforeAll
  static void enableRawResponseLogging() {
    Logger clientLogger = (Logger) LoggerFactory.getLogger(GeminiSummaryClient.class);
    clientLogger.setLevel(Level.DEBUG);
  }

  private GeminiSummaryClient client() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(10));
    requestFactory.setReadTimeout(Duration.ofSeconds(180));

    return new GeminiSummaryClient(
        RestClient.builder()
            .baseUrl(GeminiQuestionClient.BASE_URL)
            .requestFactory(requestFactory)
            .build(),
        new ObjectMapper(),
        System.getenv("GEMINI_API_KEY"),
        GeminiQuestionClient.DEFAULT_MODEL);
  }

  private Book almond() {
    return Book.builder()
        .isbn("9788936434595")
        .title("아몬드")
        .author("손원평")
        .publisher("창비")
        .publishedDate(LocalDate.of(2017, 3, 31))
        .kdcName("한국소설")
        .description(
            "감정을 느끼지 못하는 알렉시티미아를 안고 태어난 소년 윤재. 편도체가 작아 두려움도 공포도 느끼지 못하는 그는 할멈과 엄마의 보살핌 속에 살아간다."
                + " 그러나 열여섯 생일에 벌어진 사건으로 홀로 남겨지고, 정반대의 상처를 지닌 곤이를 만나면서 조금씩 세상과 부딪히기 시작한다."
                + " 감정이 없는 아이가 감정을 배워가는 과정을 담담하게 그려낸 성장소설.")
        .build();
  }

  private List<AIQuestion> seedQuestions() {
    return SeedQuestion.ordered().stream()
        .map(
            seed ->
                AIQuestion.builder()
                    .content(seed.getContent())
                    .questionOrder(seed.getQuestionOrder())
                    .build())
        .toList();
  }

  // 실제 모임에서 오갈 법한 대화. 의견이 갈리는 지점과 자기 경험을 꺼내는 지점을 일부러 섞었다.
  private List<ChatMessage> conversation() {
    Meeting meeting = Meeting.builder().book(almond()).duration(60).maxParticipants(6).build();
    ChatRoom chatRoom = ChatRoom.create(meeting);

    List<String[]> lines =
        List.of(
            new String[] {"1", "저는 읽기 전엔 감정 없는 아이 이야기라길래 좀 차가울 줄 알았는데 오히려 따뜻했어요"},
            new String[] {"2", "저는 반대로 끝까지 불편했어요. 윤재가 진짜 변한 건지 아니면 변한 척을 배운 건지 모르겠더라고요"},
            new String[] {"3", "그 지점이 저도 계속 걸렸어요. 마지막에 곤이를 찾아간 게 감정이었을까요 아니면 학습된 행동이었을까요"},
            new String[] {"1", "저는 감정이라고 봤어요. 이유를 설명 못 하는데 몸이 먼저 움직였잖아요"},
            new String[] {"2", "그건 해석 나름인 것 같아요. 저는 여전히 판단 보류입니다"},
            new String[] {"3", "별점은 저는 4점이요. 문장이 담백해서 좋았는데 후반부가 좀 빨리 끝난 느낌이에요"},
            new String[] {"1", "저도 4점. 곤이 서사가 더 있었으면 했어요"},
            new String[] {"2", "저는 3.5점이요. 설정은 좋았는데 주변 인물들이 너무 기능적으로 소비된다고 느꼈어요"},
            new String[] {"3", "가장 오래 남은 장면은 엄마가 쓰러지는 그 생일 장면이었어요. 윤재가 아무 반응도 못 하는 게 오히려 더 아팠어요"},
            new String[] {"1", "저는 할멈이 윤재한테 예쁘다고 말해주는 장면이요. 감정을 못 느끼는 애한테 계속 말을 걸어주는 게 뭔가 뭉클했어요"},
            new String[] {"2", "저는 곤이가 윤재한테 화내는 장면이 인상 깊었어요. 화를 내는 쪽이 오히려 더 상처받은 사람 같아서"},
            new String[] {"3", "밑줄 그은 문장은 감정을 모르면 상처도 없을 줄 알았다는 부분이요"},
            new String[] {"1", "저는 아몬드가 커지길 바랐다는 문장이 계속 생각나요"},
            new String[] {"2", "저는 사람들은 놀라운 일에 익숙해진다는 문장이 무서웠어요. 우리 얘기 같아서"},
            new String[] {"3", "사실 저는 이 책 읽으면서 제가 남 일에 너무 무덤덤해진 게 떠올랐어요. 뉴스 보면서 아무 느낌 없을 때가 많거든요"},
            new String[] {"1", "저도요. 공감을 못 하는 게 병인 줄 알았는데 그냥 안 하는 걸 수도 있겠다 싶었어요"},
            new String[] {"2", "그 얘기 들으니까 좀 찔리네요. 저는 오히려 감정이 너무 많아서 피곤한 쪽인데 윤재가 부러웠던 순간도 있었어요"},
            new String[] {"3", "그것도 솔직한 감상 같아요. 저는 읽고 나서 주변 사람 표정을 좀 더 보게 됐어요"},
            new String[] {
              "1", "한 문장 소개라면 감정을 못 느끼는 아이가 감정을 배우는 이야기가 아니라, 우리가 이미 잃은 걸 다시 보게 하는 이야기라고 하고 싶어요"
            },
            new String[] {"2", "저는 무감각이 편한지 아픈지 묻는 책이라고 하겠어요"});

    List<ChatMessage> messages = new ArrayList<>();
    for (String[] line : lines) {
      Member sender = Member.createSocialMember(Provider.KAKAO, "member-" + line[0]);
      ReflectionTestUtils.setField(sender, "id", Long.parseLong(line[0]));
      ReflectionTestUtils.setField(sender, "nickname", "책 먹는 여우 " + line[0]);
      messages.add(
          ChatMessage.builder().chatRoom(chatRoom).senderMember(sender).message(line[1]).build());
    }
    return messages;
  }

  @Test
  void 아몬드_모임_요약() {
    Book book = almond();
    List<AIQuestion> questions = seedQuestions();
    List<ChatMessage> messages = conversation();

    long start = System.nanoTime();
    Map<SummaryAxis, GeminiSummaryClient.SummaryDraft> drafts =
        client().generateSummaries(book, questions, messages);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    StringBuilder out = new StringBuilder();
    out.append("\n\n##### 요약 결과: ")
        .append(book.getTitle())
        .append(" (")
        .append(drafts.size())
        .append("/")
        .append(SummaryAxis.count())
        .append("축, ")
        .append(elapsedMs)
        .append("ms) #####\n");

    for (SummaryAxis axis : SummaryAxis.ordered()) {
      GeminiSummaryClient.SummaryDraft draft = drafts.get(axis);
      out.append('\n')
          .append(axis.getDisplayOrder())
          .append(". [")
          .append(axis.name())
          .append("]\n");
      if (draft == null) {
        out.append("   제목: 나눈 이야기가 적어 정리하지 못했어요 (폴백)\n   요약: (비어 있음)\n");
        continue;
      }
      out.append("   제목: ").append(draft.title()).append('\n');
      out.append("   요약: ").append(draft.content()).append('\n');
    }

    out.append("\n----- 프롬프트에 닉네임이 들어갔는지 -----\n");
    String prompt = client().buildPrompt(book, questions, messages);
    out.append(prompt.contains("책 먹는 여우") ? "닉네임 노출됨 (문제)\n" : "닉네임 없음 (정상)\n");
    out.append("라벨: ")
        .append(prompt.contains("참여자 A") ? "A " : "")
        .append(prompt.contains("참여자 B") ? "B " : "")
        .append(prompt.contains("참여자 C") ? "C" : "")
        .append('\n');

    System.out.println(out);
  }
}
