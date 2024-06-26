package com.example.tripbridgeserver.controller;

import com.example.tripbridgeserver.dto.MateCommentDTO;
import com.example.tripbridgeserver.entity.MateComment;
import com.example.tripbridgeserver.entity.MatePost;
import com.example.tripbridgeserver.entity.UserEntity;
import com.example.tripbridgeserver.repository.MateCommentRepository;
import com.example.tripbridgeserver.repository.MatePostRepository;
import com.example.tripbridgeserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;


//Mate 게시판 댓글 관련 Controller
@RestController
public class MateCommentController {

    private final MatePostRepository matePostRepository;
    private final UserRepository userRepository;
    private final MateCommentRepository mateCommentRepository;

    @Autowired
    public MateCommentController(MatePostRepository matePostRepository, UserRepository userRepository, MateCommentRepository mateCommentRepository) {
        this.matePostRepository = matePostRepository;
        this.userRepository = userRepository;
        this.mateCommentRepository = mateCommentRepository;
    }

    //Mate 게시판 id번 글에 대한 댓글 조회
    @GetMapping("/mate/{id}/comment")
    public List<MateComment> comment (@PathVariable Long id){
       MatePost matePost = matePostRepository.findById(id).orElse(null);
       if (matePost != null){
       return mateCommentRepository.findByMatePost(matePost) ; }
       else {
           return null; // 또는 예외를 처리하거나 적절한 방법으로 처리
       }
    }

    //Mate 게시판 댓글 생성
    @PostMapping("/mate/comment")
    public ResponseEntity<MateComment> createComment(@RequestBody MateCommentDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        UserEntity currentUser = userRepository.findByEmail(userEmail);
        MateComment mateComment = dto.toEntity(currentUser, matePostRepository);

        // 부모 댓글이 있는 경우
        if (dto.getParent_comment_id() != null) {
            MateComment parentComment = mateCommentRepository.findById(dto.getParent_comment_id())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found with id: " + dto.getParent_comment_id()));
            mateComment.setParentComment(parentComment);
            mateComment.setDepth(parentComment.getDepth() + 1);
            mateComment.setComment_group(parentComment.getComment_group());
        }
        else {
            Long maxCommentGroup = mateCommentRepository.findMaxCommentGroupByMatePostId(dto.getMatePost_id()); // 해당 게시물에서 가장 높은 comment_group 값을 가져옴
            mateComment.setComment_group(maxCommentGroup != null ? maxCommentGroup + 1 : 0L); // 새로운 댓글의 comment_group 을 설정
        }

        // 대댓글의 순서 설정
        Long maxOrder = mateCommentRepository.findMaxOrderOfComment(dto.getParent_comment_id());
        if (maxOrder != null) {
            mateComment.setComment_order(maxOrder + 1);
        } else {
            mateComment.setComment_order(0L); // 대댓글이 부모 댓글의 첫 번째 대댓글일 경우
        }
        MateComment savedComment = mateCommentRepository.save(mateComment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    //Mate 게시판 단일 댓글 수정
    @PatchMapping("/mate/comment/{id}")
    public ResponseEntity<MateComment> update(@PathVariable Long id, @RequestBody MateCommentDTO dto){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        UserEntity currentUser = userRepository.findByEmail(userEmail);
        MateComment mateComment = dto.toEntity(currentUser, matePostRepository);

        MateComment target = mateCommentRepository.findById(id).orElse(null);

        if(target==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        target.setMatePost(mateComment.getMatePost());
        target.setContent(mateComment.getContent());
        target.setUserEntity(mateComment.getUserEntity());
        MateComment updated = mateCommentRepository.save(target);
        return ResponseEntity.status(HttpStatus.OK).body(updated);
    }
    //Mate 게시판 단일 댓글 수정
    @DeleteMapping("/mate/comment/{id}")
    public ResponseEntity<MateComment> delete(@PathVariable Long id){
        MateComment target = mateCommentRepository.findById(id).orElse(null);
        if(target==null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        mateCommentRepository.delete(target);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
