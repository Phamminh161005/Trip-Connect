package com.tripconnect.backend.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Giữ file trên Cloudinary khớp với DB. Upload/xóa file KHÔNG nằm trong transaction của DB, nên:
 * - File vừa upload mà transaction rollback -> phải xóa file (nếu không sẽ thành file rác).
 * - File cũ cần xóa -> chỉ xóa SAU KHI DB commit thành công (nếu xóa trước mà DB rollback thì mất file).
 * Phải được gọi bên trong một @Transactional.
 */
@Component
@RequiredArgsConstructor
public class TransactionalFileCleanup {

    private final FileStorageService fileStorageService;

    public void deleteAfterCommit(String publicId, FileVisibility visibility) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.delete(publicId, visibility);
            }
        });
    }

    public void deleteOnRollback(String publicId, FileVisibility visibility) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    fileStorageService.delete(publicId, visibility);
                }
            }
        });
    }
}
