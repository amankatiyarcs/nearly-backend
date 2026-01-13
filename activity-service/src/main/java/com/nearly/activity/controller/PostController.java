package com.nearly.activity.controller;

import com.nearly.activity.model.*;
import com.nearly.activity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostRepository postRepository;
    private final PollRepository pollRepository;
    private final QuestionRepository questionRepository;
    private final DiscussionRepository discussionRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PollVoteRepository pollVoteRepository;
    private final AnswerRepository answerRepository;

    // ============ POSTS ============

    @GetMapping("/posts")
    public ResponseEntity<List<Post>> getPosts(@RequestParam(required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)));
        }
        return ResponseEntity.ok(postRepository.findAll());
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<Post> getPost(@PathVariable String id) {
        return postRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/posts")
    @Transactional
    public ResponseEntity<Post> createPost(@RequestBody Map<String, Object> body) {
        Post post = Post.builder()
            .userId((String) body.get("userId"))
            .content((String) body.get("content"))
            .imageUrl((String) body.get("imageUrl"))
            .videoUrl((String) body.get("videoUrl"))
            .visibility((String) body.getOrDefault("visibility", "public"))
            .likesCount(0)
            .commentsCount(0)
            .sharesCount(0)
            .createdAt(Instant.now())
            .build();
        
        Post saved = postRepository.save(post);
        log.info("Created post {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/posts/{id}")
    @Transactional
    public ResponseEntity<Post> updatePost(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return postRepository.findById(id)
            .map(post -> {
                if (updates.containsKey("content")) post.setContent((String) updates.get("content"));
                if (updates.containsKey("imageUrl")) post.setImageUrl((String) updates.get("imageUrl"));
                if (updates.containsKey("videoUrl")) post.setVideoUrl((String) updates.get("videoUrl"));
                if (updates.containsKey("visibility")) post.setVisibility((String) updates.get("visibility"));
                post.setUpdatedAt(Instant.now());
                return ResponseEntity.ok(postRepository.save(post));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable String id) {
        postRepository.deleteById(id);
        commentRepository.deleteByTargetTypeAndTargetId("post", id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/posts/{id}/like")
    @Transactional
    public ResponseEntity<Post> likePost(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return postRepository.findById(id)
            .map(post -> {
                String userId = (String) body.get("userId");
                boolean increment = body.get("increment") == null || (Boolean) body.get("increment");
                
                if (increment) {
                    if (!likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, "post", id)) {
                        Like like = Like.builder()
                            .userId(userId)
                            .targetType("post")
                            .targetId(id)
                            .createdAt(Instant.now())
                            .build();
                        likeRepository.save(like);
                        post.setLikesCount(post.getLikesCount() + 1);
                    }
                } else {
                    likeRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, "post", id);
                    post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
                }
                
                return ResponseEntity.ok(postRepository.save(post));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<Map<String, Object>>> getPostComments(@PathVariable String id) {
        List<Comment> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("post", id);
        return ResponseEntity.ok(comments.stream()
            .map(this::commentToMap)
            .collect(Collectors.toList()));
    }

    @PostMapping("/posts/{id}/comments")
    @Transactional
    public ResponseEntity<Map<String, Object>> addPostComment(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Comment comment = Comment.builder()
            .userId((String) body.get("userId"))
            .targetType("post")
            .targetId(id)
            .content((String) body.get("content"))
            .parentCommentId((String) body.get("parentCommentId"))
            .likesCount(0)
            .repliesCount(0)
            .createdAt(Instant.now())
            .build();
        
        Comment saved = commentRepository.save(comment);
        
        postRepository.findById(id).ifPresent(post -> {
            post.setCommentsCount(post.getCommentsCount() + 1);
            postRepository.save(post);
        });
        
        return ResponseEntity.ok(commentToMap(saved));
    }

    // ============ POLLS ============

    @GetMapping("/polls")
    public ResponseEntity<List<Poll>> getPolls(@RequestParam(required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(pollRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)));
        }
        return ResponseEntity.ok(pollRepository.findAll());
    }

    @GetMapping("/polls/{id}")
    public ResponseEntity<Poll> getPoll(@PathVariable String id) {
        return pollRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/polls")
    @Transactional
    public ResponseEntity<Poll> createPoll(@RequestBody Map<String, Object> body) {
        Poll poll = Poll.builder()
            .userId((String) body.get("userId"))
            .question((String) body.get("question"))
            .optionsJson(body.get("options") != null ? body.get("options").toString() : null)
            .totalVotes(0)
            .allowMultiple((Boolean) body.get("allowMultiple"))
            .createdAt(Instant.now())
            .build();
        
        Poll saved = pollRepository.save(poll);
        log.info("Created poll {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/polls/{id}/vote")
    @Transactional
    public ResponseEntity<Poll> votePoll(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return pollRepository.findById(id)
            .map(poll -> {
                String userId = (String) body.get("userId");
                String optionId = (String) body.get("optionId");
                
                if (!pollVoteRepository.existsByPollIdAndUserId(id, userId)) {
                    PollVote vote = PollVote.builder()
                        .pollId(id)
                        .userId(userId)
                        .optionId(optionId)
                        .votedAt(Instant.now())
                        .build();
                    pollVoteRepository.save(vote);
                    poll.setTotalVotes(poll.getTotalVotes() + 1);
                    poll = pollRepository.save(poll);
                }
                
                return ResponseEntity.ok(poll);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/polls/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deletePoll(@PathVariable String id) {
        pollRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ============ QUESTIONS ============

    @GetMapping("/questions")
    public ResponseEntity<List<Question>> getQuestions(@RequestParam(required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(questionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)));
        }
        return ResponseEntity.ok(questionRepository.findAll());
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestion(@PathVariable String id) {
        return questionRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/questions")
    @Transactional
    public ResponseEntity<Question> createQuestion(@RequestBody Map<String, Object> body) {
        Question question = Question.builder()
            .userId((String) body.get("userId"))
            .title((String) body.get("title"))
            .body((String) body.get("body"))
            .tags((String) body.get("tags"))
            .upvotesCount(0)
            .answersCount(0)
            .viewsCount(0)
            .isResolved(false)
            .createdAt(Instant.now())
            .build();
        
        Question saved = questionRepository.save(question);
        log.info("Created question {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/questions/{id}/upvote")
    @Transactional
    public ResponseEntity<Question> upvoteQuestion(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return questionRepository.findById(id)
            .map(question -> {
                String userId = (String) body.get("userId");
                
                if (!likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, "question", id)) {
                    Like like = Like.builder()
                        .userId(userId)
                        .targetType("question")
                        .targetId(id)
                        .createdAt(Instant.now())
                        .build();
                    likeRepository.save(like);
                    question.setUpvotesCount(question.getUpvotesCount() + 1);
                    question = questionRepository.save(question);
                }
                
                return ResponseEntity.ok(question);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/questions/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteQuestion(@PathVariable String id) {
        questionRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/questions/{id}/answers")
    public ResponseEntity<List<Map<String, Object>>> getAnswers(@PathVariable String id) {
        List<Answer> answers = answerRepository.findByQuestionIdOrderByCreatedAtDesc(id);
        return ResponseEntity.ok(answers.stream()
            .map(this::answerToMap)
            .collect(Collectors.toList()));
    }

    @PostMapping("/questions/{id}/answers")
    @Transactional
    public ResponseEntity<Map<String, Object>> addAnswer(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Answer answer = Answer.builder()
            .questionId(id)
            .userId((String) body.get("userId"))
            .content((String) body.get("content"))
            .upvotesCount(0)
            .isAccepted(false)
            .createdAt(Instant.now())
            .build();
        
        Answer saved = answerRepository.save(answer);
        
        questionRepository.findById(id).ifPresent(question -> {
            question.setAnswersCount(question.getAnswersCount() + 1);
            questionRepository.save(question);
        });
        
        return ResponseEntity.ok(answerToMap(saved));
    }

    // ============ DISCUSSIONS ============

    @GetMapping("/discussions")
    public ResponseEntity<List<Discussion>> getDiscussions(@RequestParam(required = false) Integer limit) {
        if (limit != null) {
            return ResponseEntity.ok(discussionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)));
        }
        return ResponseEntity.ok(discussionRepository.findAll());
    }

    @GetMapping("/discussions/{id}")
    public ResponseEntity<Discussion> getDiscussion(@PathVariable String id) {
        return discussionRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/discussions")
    @Transactional
    public ResponseEntity<Discussion> createDiscussion(@RequestBody Map<String, Object> body) {
        Discussion discussion = Discussion.builder()
            .userId((String) body.get("userId"))
            .title((String) body.get("title"))
            .content((String) body.get("content"))
            .category((String) body.get("category"))
            .likesCount(0)
            .commentsCount(0)
            .viewsCount(0)
            .isPinned(false)
            .isLocked(false)
            .createdAt(Instant.now())
            .build();
        
        Discussion saved = discussionRepository.save(discussion);
        log.info("Created discussion {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/discussions/{id}/like")
    @Transactional
    public ResponseEntity<Discussion> likeDiscussion(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return discussionRepository.findById(id)
            .map(discussion -> {
                String userId = (String) body.get("userId");
                
                if (!likeRepository.existsByUserIdAndTargetTypeAndTargetId(userId, "discussion", id)) {
                    Like like = Like.builder()
                        .userId(userId)
                        .targetType("discussion")
                        .targetId(id)
                        .createdAt(Instant.now())
                        .build();
                    likeRepository.save(like);
                    discussion.setLikesCount(discussion.getLikesCount() + 1);
                    discussion = discussionRepository.save(discussion);
                }
                
                return ResponseEntity.ok(discussion);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/discussions/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteDiscussion(@PathVariable String id) {
        discussionRepository.deleteById(id);
        commentRepository.deleteByTargetTypeAndTargetId("discussion", id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/discussions/{id}/comments")
    public ResponseEntity<List<Map<String, Object>>> getDiscussionComments(@PathVariable String id) {
        List<Comment> comments = commentRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("discussion", id);
        return ResponseEntity.ok(comments.stream()
            .map(this::commentToMap)
            .collect(Collectors.toList()));
    }

    @PostMapping("/discussions/{id}/comments")
    @Transactional
    public ResponseEntity<Map<String, Object>> addDiscussionComment(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Comment comment = Comment.builder()
            .userId((String) body.get("userId"))
            .targetType("discussion")
            .targetId(id)
            .content((String) body.get("content"))
            .parentCommentId((String) body.get("parentCommentId"))
            .likesCount(0)
            .repliesCount(0)
            .createdAt(Instant.now())
            .build();
        
        Comment saved = commentRepository.save(comment);
        
        discussionRepository.findById(id).ifPresent(discussion -> {
            discussion.setCommentsCount(discussion.getCommentsCount() + 1);
            discussionRepository.save(discussion);
        });
        
        return ResponseEntity.ok(commentToMap(saved));
    }

    // ============ HELPER METHODS ============

    private Map<String, Object> commentToMap(Comment comment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("userId", comment.getUserId());
        map.put("targetType", comment.getTargetType());
        map.put("targetId", comment.getTargetId());
        map.put("parentCommentId", comment.getParentCommentId());
        map.put("content", comment.getContent());
        map.put("likesCount", comment.getLikesCount());
        map.put("repliesCount", comment.getRepliesCount());
        map.put("createdAt", comment.getCreatedAt().toString());
        if (comment.getUpdatedAt() != null) {
            map.put("updatedAt", comment.getUpdatedAt().toString());
        }
        return map;
    }

    private Map<String, Object> answerToMap(Answer answer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", answer.getId());
        map.put("questionId", answer.getQuestionId());
        map.put("userId", answer.getUserId());
        map.put("content", answer.getContent());
        map.put("upvotesCount", answer.getUpvotesCount());
        map.put("isAccepted", answer.getIsAccepted());
        map.put("createdAt", answer.getCreatedAt().toString());
        if (answer.getUpdatedAt() != null) {
            map.put("updatedAt", answer.getUpdatedAt().toString());
        }
        return map;
    }
}
